package org.example.paperwise.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.paperwise.Mapper.NoteMapper;
import org.example.paperwise.Mapper.NoteOrganizationMapper;
import org.example.paperwise.entry.Note;
import org.example.paperwise.entry.NoteOrganization;
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
        NoteOrganization item = organizationMapper.selectById(organizationId);
        try {
            String prompt = "请把下面的学习笔记整理成适合复习的 Markdown。保留所有事实和公式，不要删除关键内容；输出清晰的标题、要点、定义、例子和复习提示。数学公式使用 KaTeX 兼容的 $...$ 或 $$...$$。只输出 Markdown。\n\n" + content;
            item.setOrganizedContent(aiProviderService.chat(userId, prompt));
            item.setStatus("SUCCESS");
        } catch (Exception e) {
            item.setStatus("FAILED");
            item.setErrorMessage(e.getMessage() == null ? "AI 整理失败" : e.getMessage());
        }
        item.setUpdatedAt(LocalDateTime.now());
        organizationMapper.updateById(item);
    }
}
