package cn.exitcode.richpeasants.common.storage;

import cn.exitcode.richpeasants.common.exception.BusinessException;
import cn.exitcode.richpeasants.common.result.ResultCode;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class MinioStorageService {

    private static final Set<String> IMAGE_EXT = Set.of("png", "jpg", "jpeg", "gif", "webp", "svg");

    private final MinioClient minioClient;
    private final MinioProperties properties;

    public MinioStorageService(MinioClient minioClient, MinioProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    public String upload(Long kbId, MultipartFile file) {
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String safeName = sanitizeFilename(original);
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String objectKey = String.format("%s/%d/%s/%s_%s",
                trimSlash(properties.getBasePath()),
                kbId,
                date,
                UUID.randomUUID().toString().replace("-", ""),
                safeName);
        putObject(objectKey, file);
        return objectKey;
    }

    /**
     * 上传站点资源（如 Logo），返回可直接访问的公开 URL。
     */
    public String uploadSiteAsset(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "上传文件不能为空");
        }
        String original = file.getOriginalFilename() == null ? "asset" : file.getOriginalFilename();
        String ext = extension(original);
        if (!IMAGE_EXT.contains(ext)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Logo 仅支持 png/jpg/jpeg/gif/webp/svg");
        }
        String safeName = sanitizeFilename(original);
        String objectKey = String.format("%s/%s/%s_%s",
                trimSlash(properties.getBasePath()),
                StringUtils.hasText(folder) ? folder : "site",
                UUID.randomUUID().toString().replace("-", ""),
                safeName);
        putObject(objectKey, file);
        return buildPublicUrl(objectKey);
    }

    public void delete(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return;
        }
        String key = normalizeObjectKey(objectKey);
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(key)
                    .build());
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "删除 MinIO 文件失败: " + ex.getMessage());
        }
    }

    /**
     * 下载对象为字节数组（入库解析用）。
     */
    public byte[] downloadBytes(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "对象路径为空");
        }
        String key = normalizeObjectKey(objectKey);
        try (InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(properties.getBucket())
                .object(key)
                .build());
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            stream.transferTo(output);
            return output.toByteArray();
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "从 MinIO 下载文件失败: " + ex.getMessage());
        }
    }

    public String buildPublicUrl(String objectKey) {
        String endpoint = trimTrailingSlash(properties.getEndpoint());
        String bucket = properties.getBucket();
        if (!StringUtils.hasText(objectKey)) {
            return endpoint + "/" + bucket + "/";
        }
        return endpoint + "/" + bucket + "/" + objectKey.replaceAll("^/+", "");
    }

    private String normalizeObjectKey(String objectKey) {
        String key = objectKey;
        String prefix = buildPublicUrl("");
        if (key.startsWith(prefix)) {
            key = key.substring(prefix.length());
        }
        return key.replaceAll("^/+", "");
    }

    private void putObject(String objectKey, MultipartFile file) {
        try {
            // 桶由运维预先创建。Service Account 常有 PutObject 权限但无 HeadBucket/MakeBucket，
            // 上传前探测桶会导致 Access Denied，因此直接 PutObject。
            String contentType = StringUtils.hasText(file.getContentType())
                    ? file.getContentType()
                    : "application/octet-stream";
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(properties.getBucket())
                        .object(objectKey)
                        .stream(inputStream, file.getSize(), -1)
                        .contentType(contentType)
                        .build());
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "文件上传到 MinIO 失败: " + ex.getMessage());
        }
    }

    private String sanitizeFilename(String original) {
        return original.replaceAll("[\\\\/\\s]+", "_");
    }

    private String extension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx < 0) {
            return "";
        }
        return filename.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    private String trimSlash(String path) {
        if (!StringUtils.hasText(path)) {
            return "kb-qa";
        }
        String value = path.trim();
        while (value.startsWith("/")) {
            value = value.substring(1);
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value.isEmpty() ? "kb-qa" : value;
    }

    private String trimTrailingSlash(String endpoint) {
        if (!StringUtils.hasText(endpoint)) {
            return "";
        }
        String value = endpoint.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
