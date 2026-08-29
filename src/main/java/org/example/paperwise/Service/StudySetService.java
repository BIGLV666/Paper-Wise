package org.example.paperwise.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.paperwise.Mapper.CardMapper;
import org.example.paperwise.Mapper.StudySetMapper;
import org.example.paperwise.entry.Card;
import org.example.paperwise.entry.StudySet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class StudySetService {
    private final StudySetMapper studySetMapper;
    private final CardMapper cardMapper;

    public StudySetService(StudySetMapper studySetMapper, CardMapper cardMapper) {
        this.studySetMapper = studySetMapper;
        this.cardMapper = cardMapper;
    }

    public List<StudySet> list(Long userId) {
        return studySetMapper.selectList(new LambdaQueryWrapper<StudySet>()
                .eq(StudySet::getUserId, userId)
                .orderByDesc(StudySet::getUpdatedAt));
    }

    public StudySet get(Long userId, Long studySetId) {
        StudySet set = studySetMapper.selectById(studySetId);
        requireOwner(set, userId);
        return set;
    }

    @Transactional
    public StudySet create(Long userId, StudySet incoming) {
        validate(incoming);
        validateCards(userId, incoming.getCardIds());
        StudySet set = new StudySet();
        set.setUserId(userId);
        set.setName(incoming.getName().trim());
        set.setDescription(incoming.getDescription());
        set.setCardIds(normalizeIds(incoming.getCardIds()));
        set.setCreatedAt(LocalDateTime.now());
        set.setUpdatedAt(LocalDateTime.now());
        studySetMapper.insert(set);
        return set;
    }

    @Transactional
    public StudySet update(Long userId, Long studySetId, StudySet incoming) {
        StudySet set = get(userId, studySetId);
        validate(incoming);
        validateCards(userId, incoming.getCardIds());
        set.setName(incoming.getName().trim());
        set.setDescription(incoming.getDescription());
        set.setCardIds(normalizeIds(incoming.getCardIds()));
        set.setUpdatedAt(LocalDateTime.now());
        studySetMapper.updateById(set);
        return set;
    }

    @Transactional
    public void delete(Long userId, Long studySetId) {
        StudySet set = get(userId, studySetId);
        studySetMapper.deleteById(set.getStudySetId());
    }

    public List<Card> cards(Long userId, Long studySetId) {
        StudySet set = get(userId, studySetId);
        if (set.getCardIds() == null || set.getCardIds().isEmpty()) return List.of();
        List<Card> cards = cardMapper.selectBatchIds(set.getCardIds());
        return cards.stream().filter(card -> Objects.equals(card.getUserid(), userId)).toList();
    }

    private void validate(StudySet set) {
        if (set == null || set.getName() == null || set.getName().isBlank()) {
            throw new IllegalArgumentException("题单名称不能为空");
        }
        if (set.getName().trim().length() > 100) throw new IllegalArgumentException("题单名称不能超过100字");
    }

    private void validateCards(Long userId, List<Integer> cardIds) {
        for (Integer cardId : normalizeIds(cardIds)) {
            Card card = cardMapper.selectById(cardId);
            if (card == null || !Objects.equals(card.getUserid(), userId)) {
                throw new IllegalArgumentException("题单中包含无权访问的卡片");
            }
        }
    }

    private List<Integer> normalizeIds(List<Integer> cardIds) {
        return cardIds == null ? new ArrayList<>() : cardIds.stream().filter(Objects::nonNull).distinct().toList();
    }

    private void requireOwner(StudySet set, Long userId) {
        if (set == null) throw new IllegalArgumentException("题单不存在");
        if (!Objects.equals(set.getUserId(), userId)) throw new IllegalArgumentException("无权访问该题单");
    }
}
