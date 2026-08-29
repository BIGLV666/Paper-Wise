package org.example.paperwise.Controller;

import io.github.biglv666.guard.idempotent.Idempotent;
import org.example.paperwise.Dto.Result;
import org.example.paperwise.Service.StudySetService;
import org.example.paperwise.entry.Card;
import org.example.paperwise.entry.StudySet;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/paperwise/study-sets")
public class StudySetController {
    private final StudySetService studySetService;

    public StudySetController(StudySetService studySetService) {
        this.studySetService = studySetService;
    }

    @GetMapping
    public Result<List<StudySet>> list(@RequestAttribute Long userid) {
        return Result.success(studySetService.list(userid));
    }

    @GetMapping("/{id}")
    public Result<StudySet> get(@RequestAttribute Long userid, @PathVariable Long id) {
        return Result.success(studySetService.get(userid, id));
    }

    @PostMapping
    @Idempotent(key = "#userid + ':' + #studySet.name", ttl = 10, message = "同名学习集正在创建中，请勿重复提交")
    public Result<StudySet> create(@RequestAttribute Long userid, @RequestBody StudySet studySet) {
        return Result.success(studySetService.create(userid, studySet));
    }

    @PutMapping("/{id}")
    public Result<StudySet> update(@RequestAttribute Long userid, @PathVariable Long id,
                                   @RequestBody StudySet studySet) {
        return Result.success(studySetService.update(userid, id, studySet));
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@RequestAttribute Long userid, @PathVariable Long id) {
        studySetService.delete(userid, id);
        return Result.success("删除成功");
    }

    @GetMapping("/{id}/cards")
    public Result<List<Card>> cards(@RequestAttribute Long userid, @PathVariable Long id) {
        return Result.success(studySetService.cards(userid, id));
    }
}
