package org.example.paperwise.Until;

public  class  BuildPromptUntil {

    public static final String buildPrompt(String text) {
        if(text.length()>5000){
            text=text.substring(0,5000);
        }
        return """
        你是一个学习卡片生成助手。请根据以下文本内容，将文本按照以下形式拆分
        
        每张卡片包含：
        - title：卡片标题/问题
        - questionType:题目所属大的类别
        - cardType：类型（SINGLE=单选题、MULTIPLE=多选题、TRUE_FALSE=判断题、ESSAY=问答题）
        - options：选项数组（选择题必填，其他类型可为空数组）
        - answer：正确答案
        - explanation：解析说明
        - cardDifficulty：难度（EASY、MEDIUM、HARD）
        
        
        输出必须是 JSON 数组格式，不要有其他文字。如果文本有截断只返回未截断部分.questionType要求中文如果是java或者mysql等等,可以直接写java等，
        
        
        示例：
        [
                  {
                       "title": "Java 的特点是什么？",
                       "questionType": "JAVA",
                       "cardType": "SINGLE",
                       "options": ["跨平台", "面向对象", "指针操作", "自动内存管理"],
                       "answer": "跨平台",
                       "explanation": "Java 通过 JVM 实现跨平台",
                       "cardDifficulty": "EASY"
                     },
                     {
                       "title": "以下哪些是 JVM 的内存区域？",
                       "questionType": "JAVA",
                       "cardType": "MULTIPLE",
                       "options": ["堆", "栈", "方法区", "寄存器", "CPU缓存"],
                       "answer": ["堆", "栈", "方法区"],
                       "explanation": "JVM 内存区域包括堆、栈、方法区、程序计数器",
                       "cardDifficulty": "MEDIUM"
                     }
                        ]
        
        文本内容：
        """ + text;
    }
}
