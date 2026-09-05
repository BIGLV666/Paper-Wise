package org.example.paperwise.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.github.biglv666.statekit.FireArg;
import io.github.biglv666.statekit.StateMachine;
import io.github.biglv666.statekit.exception.IllegalTransitionException;
import org.example.paperwise.Mapper.NoteMapper;
import org.example.paperwise.Mapper.NoteOrganizationMapper;
import org.example.paperwise.entry.Note;
import org.example.paperwise.entry.NoteOrganization;
import org.example.paperwise.enums.NoteOrganizeStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class NoteService {
    @Autowired private NoteMapper noteMapper;
    @Autowired private NoteOrganizationMapper organizationMapper;
    @Autowired private AiProviderService aiProviderService;
    // state-kit 生成的状态机 Bean（machine=noteOrganize）：note_ai_organization.status 唯一写入口
    @Autowired private StateMachine<NoteOrganizeStatus, Long> noteOrganize;

    public List<Note> list(Long userId) {
        return noteMapper.selectList(new QueryWrapper<Note>().eq("user_id", userId).orderByDesc("updated_at"));
    }

    public Note get(Long userId, Long noteId) {
        Note note = noteMapper.selectById(noteId);
        if (note == null || !userId.equals(note.getUserId())) throw new IllegalArgumentException("笔记不存在");
        return note;
    }

    @Transactional
    public Note save(Long userId, Note incoming) {
        Note note = incoming.getNoteId() == null ? new Note() : get(userId, incoming.getNoteId());
        note.setUserId(userId);
        note.setTitle(incoming.getTitle() == null || incoming.getTitle().isBlank() ? "未命名笔记" : incoming.getTitle());
        note.setContent(incoming.getContent() == null ? "" : incoming.getContent());
        note.setFormat("markdown".equalsIgnoreCase(incoming.getFormat()) ? "markdown" : "text");
        note.setSourceName(incoming.getSourceName());
        note.setUpdatedAt(LocalDateTime.now());
        if (note.getCreatedAt() == null) note.setCreatedAt(LocalDateTime.now());
        if (note.getNoteId() == null) noteMapper.insert(note); else noteMapper.updateById(note);
        return note;
    }

    @Transactional
    public void delete(Long userId, Long noteId) {
        get(userId, noteId);
        noteMapper.deleteById(noteId);
    }

    @Transactional
    public NoteOrganization startOrganize(Long userId, Long noteId) {
        Note note = get(userId, noteId);
        NoteOrganization item = new NoteOrganization();
        item.setNoteId(noteId);
        item.setUserId(userId);
        item.setStatus("PROCESSING");
        item.setSourceContent(note.getContent());
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        organizationMapper.insert(item);
        CompletableFuture.runAsync(() -> processOrganization(item.getOrganizationId(), userId, note.getContent()));
        return item;
    }

    public List<NoteOrganization> organizations(Long userId, Long noteId) {
        get(userId, noteId);
        return organizationMapper.selectList(new QueryWrapper<NoteOrganization>()
                .eq("user_id", userId).eq("note_id", noteId).orderByDesc("created_at"));
    }

    public NoteOrganization organization(Long userId, Long organizationId) {
        NoteOrganization item = organizationMapper.selectById(organizationId);
        if (item == null || !userId.equals(item.getUserId())) throw new IllegalArgumentException("整理记录不存在");
        return item;
    }

    private void processOrganization(Long organizationId, Long userId, String content) {
        // 状态流转唯一写入口：PROCESSING → SUCCESS/FAILED 由状态机 CAS 完成，
        // 防止重复回调/并发整理把状态改乱；organized_content、error_message 经 set 与 status 同条 SQL 落库
        try {
            String prompt = "请把下面的学习笔记整理成适合复习的 Markdown。保留所有事实和公式，不要删除关键内容；输出清晰的标题、要点、定义、例子和复习提示。数学公式使用 KaTeX 兼容的 $...$ 或 $$...$$。只输出 Markdown。\n\n" + content;
            String organized = aiProviderService.chat(userId, prompt);
            noteOrganize.fire(organizationId, "SUCCEED",
                    FireArg.set("organized_content", organized),
                    FireArg.set("error_message", null));
        } catch (IllegalTransitionException e) {
            // 任务不在 PROCESSING（已被处理/取消）：流转不可能也不应发生，忽略
            return;
        } catch (Exception e) {
            String message = e.getMessage() == null ? "AI 整理失败" : e.getMessage();
            try {
                noteOrganize.fire(organizationId, "FAIL",
                        FireArg.set("error_message", message));
            } catch (IllegalTransitionException ignored) {
                // 并发下记录已终态，FAIL 流转不成立，忽略
            }
        }
    }
}
