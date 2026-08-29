package org.example.paperwise.Controller;

import org.example.paperwise.Dto.Result;
import org.example.paperwise.Service.NoteService;
import org.example.paperwise.entry.Note;
import org.example.paperwise.entry.NoteOrganization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/paperwise/notes")
public class NoteController {
    @Autowired
    private NoteService noteService;

    @GetMapping
    public Result<List<Note>> list(@RequestAttribute Long userid) { return Result.success(noteService.list(userid)); }

    @GetMapping("/{noteId}")
    public Result<Note> get(@RequestAttribute Long userid, @PathVariable Long noteId) { return Result.success(noteService.get(userid, noteId)); }

    @PostMapping
    public Result<Note> create(@RequestAttribute Long userid, @RequestBody Note note) { return Result.success(noteService.save(userid, note)); }

    @PutMapping("/{noteId}")
    public Result<Note> update(@RequestAttribute Long userid, @PathVariable Long noteId, @RequestBody Note note) {
        note.setNoteId(noteId);
        return Result.success(noteService.save(userid, note));
    }

    @DeleteMapping("/{noteId}")
    public Result<String> delete(@RequestAttribute Long userid, @PathVariable Long noteId) {
        noteService.delete(userid, noteId);
        return Result.success("success");
    }

    @PostMapping("/{noteId}/organize")
    public Result<NoteOrganization> organize(@RequestAttribute Long userid, @PathVariable Long noteId) {
        return Result.success(noteService.startOrganize(userid, noteId));
    }

    @GetMapping("/{noteId}/organizations")
    public Result<List<NoteOrganization>> organizations(@RequestAttribute Long userid, @PathVariable Long noteId) {
        return Result.success(noteService.organizations(userid, noteId));
    }

    @GetMapping("/organizations/{organizationId}")
    public Result<NoteOrganization> organization(@RequestAttribute Long userid, @PathVariable Long organizationId) {
        return Result.success(noteService.organization(userid, organizationId));
    }
}
