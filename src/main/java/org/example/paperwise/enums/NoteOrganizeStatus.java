package org.example.paperwise.enums;

/**
 * 笔记 AI 整理任务状态机状态。
 *
 * <p>状态只能经由 state-kit 状态机（machine=noteOrganize）流转，
 * 业务代码不得直接 UPDATE note_ai_organization.status 列（铁律）。</p>
 */
public enum NoteOrganizeStatus {
    /** AI 整理进行中（初始状态，插入记录时写入） */
    PROCESSING,
    /** AI 整理成功，organized_content 已写入 */
    SUCCESS,
    /** AI 整理失败，error_message 已写入 */
    FAILED
}
