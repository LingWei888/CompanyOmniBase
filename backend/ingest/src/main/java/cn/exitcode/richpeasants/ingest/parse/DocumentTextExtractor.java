package cn.exitcode.richpeasants.ingest.parse;

import cn.exitcode.richpeasants.common.exception.BusinessException;
import cn.exitcode.richpeasants.common.result.ResultCode;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * 文档正文提取。
 * <p>
 * 注意：{@code Tika#parseToString} 默认最多保留约 10 万字符，长 PDF 会被静默截断；
 * 这里改为无限制提取。PDF 内嵌图片中的文字属于 OCR，标准文本层解析拿不到。
 */
@Component
public class DocumentTextExtractor {

    /** -1 = 不限制提取字符数（避免长 PDF 被默认 10 万字截断） */
    private static final int WRITE_LIMIT = -1;

    private final Tika tika = createTika();
    private final Parser parser = new AutoDetectParser();

    private static Tika createTika() {
        Tika instance = new Tika();
        instance.setMaxStringLength(WRITE_LIMIT);
        return instance;
    }

    public String extract(byte[] bytes, String filename, String contentType) {
        if (bytes == null || bytes.length == 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件内容为空，无法解析");
        }
        String ext = extension(filename);
        try {
            if ("txt".equals(ext) || "md".equals(ext) || "markdown".equals(ext)) {
                return normalize(new String(bytes, StandardCharsets.UTF_8));
            }
            String text = extractWithUnlimitedLimit(bytes, filename, contentType);
            String normalized = normalize(text);
            if (!StringUtils.hasText(normalized)) {
                throw new BusinessException(ResultCode.BAD_REQUEST,
                        "未能从文件中提取到有效文本（若为扫描件/图片型 PDF，需要 OCR，当前不支持）");
            }
            return normalized;
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException | TikaException | SAXException ex) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "文档解析失败: " + ex.getMessage());
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "文档解析失败: " + ex.getMessage());
        }
    }

    private String extractWithUnlimitedLimit(byte[] bytes, String filename, String contentType)
            throws IOException, SAXException, TikaException {
        Metadata metadata = new Metadata();
        if (StringUtils.hasText(filename)) {
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, filename);
        }
        if (StringUtils.hasText(contentType)) {
            metadata.set(Metadata.CONTENT_TYPE, contentType);
        }
        // BodyContentHandler(-1) 关闭写限制；旧写法 parseToString 默认只会保留前 10 万字
        BodyContentHandler handler = new BodyContentHandler(WRITE_LIMIT);
        ParseContext context = new ParseContext();
        context.set(Parser.class, parser);
        try (InputStream stream = new ByteArrayInputStream(bytes)) {
            parser.parse(stream, handler, metadata, context);
        }
        String text = handler.toString();
        if (!StringUtils.hasText(text)) {
            try (InputStream stream = new ByteArrayInputStream(bytes)) {
                text = tika.parseToString(stream, metadata);
            }
        }
        return text;
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        String value = text.replace("\r\n", "\n").replace('\r', '\n');
        value = value.replaceAll("[\\t\\x0B\\f]+", " ");
        value = value.replaceAll(" *\\n *", "\n");
        value = value.replaceAll("\\n{3,}", "\n\n");
        return value.trim();
    }

    private String extension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
