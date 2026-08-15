package cn.exitcode.richpeasants.ingest.chunk;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 按字符长度滑动窗口切分，优先在段落/换行/句号处断开。
 */
@Component
public class TextChunker {

    public List<String> chunk(String text, int chunkSize, int overlap) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        int size = Math.max(chunkSize, 1);
        int ov = Math.max(0, Math.min(overlap, size - 1));
        String content = text.trim();
        if (content.length() <= size) {
            return List.of(content);
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + size, content.length());
            if (end < content.length()) {
                int breakAt = findBreak(content, start, end);
                if (breakAt > start) {
                    end = breakAt;
                }
            }
            String piece = content.substring(start, end).trim();
            if (!piece.isEmpty()) {
                chunks.add(piece);
            }
            if (end >= content.length()) {
                break;
            }
            int next = end - ov;
            if (next <= start) {
                next = end;
            }
            start = next;
        }
        return chunks;
    }

    private int findBreak(String content, int start, int end) {
        // 优先段落，其次换行，再次中英文句号
        int paragraph = content.lastIndexOf("\n\n", end);
        if (paragraph > start + (end - start) / 3) {
            return paragraph + 2;
        }
        int newline = content.lastIndexOf('\n', end);
        if (newline > start + (end - start) / 3) {
            return newline + 1;
        }
        int period = Math.max(
                content.lastIndexOf('。', end),
                Math.max(content.lastIndexOf('.', end), content.lastIndexOf('！', end))
        );
        if (period > start + (end - start) / 3) {
            return period + 1;
        }
        return end;
    }
}
