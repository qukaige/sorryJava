package com.lzh.service;

import com.google.common.base.Splitter;
import com.lzh.entity.Subtitles;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by lizhihao on 2018/3/11.
 */
@Service
@Getter
@Setter
@ConfigurationProperties(prefix = "cache.template")
public class GifService {

    private static final Logger logger = LoggerFactory.getLogger(GifService.class);

    private String tempPath;

    public String renderGif(Subtitles subtitles) throws Exception {
        String assPath = renderAss(subtitles);
//        String gifPath = Paths.get(tempPath).resolve(UUID.randomUUID() + ".gif").toString();
        String gifPath = tempPath + File.separator + subtitles.getTemplateName() + File.separator + UUID.randomUUID() + ".gif";
        String videoPath = tempPath + File.separator + subtitles.getTemplateName() + File.separator + "template.mp4";
        String cmd = String.format("ffmpeg -i %s -r 6 -vf ass=%s,scale=300:-1 -y %s", videoPath, assPath, gifPath);
        if ("simple".equals(subtitles.getMode())) {
//            cmd = String.format("ffmpeg -i %s -r 2 -vf ass=%s,scale=250:-1 -f gif - |gifsicle --optimize=3 --delay=20 > %s ", videoPath, assPath, gifPath);
            cmd = String.format("ffmpeg -i %s -r 5 -vf ass=%s,scale=180:-1 -y %s ", videoPath, assPath, gifPath);
        }
        logger.info("cmd: {}", cmd);
        try {
            Process exec = Runtime.getRuntime().exec(cmd);
            final InputStream is1 = exec.getInputStream();
            new Thread(() -> {
                BufferedReader br = new BufferedReader(new InputStreamReader(is1));
                try{
                    while(br.readLine() != null) ;
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
            }).start();
            InputStream is2 = exec.getErrorStream();
            BufferedReader br2 = new BufferedReader(new InputStreamReader(is2));
            while(br2.readLine() != null){}
            int i = exec.waitFor();
            logger.info("执行成功： " + i);
        } catch (Exception e) {
            logger.error("生成gif报错：{}", e);
        }
        return gifPath;
    }

    public static void main(String[] args) {
        String s = Paths.get("D:/aaa/").resolve("/template.mp4").toString();
        System.out.println(s);
    }
    private String renderAss(Subtitles subtitles) throws Exception {
        String path = tempPath + File.separator + subtitles.getTemplateName() + File.separator + UUID.randomUUID().toString().replace("-", "") + ".ass";

        Configuration cfg = new Configuration(Configuration.VERSION_2_3_27);
        cfg.setDefaultEncoding("UTF-8");
        cfg.setDirectoryForTemplateLoading(Paths.get(tempPath).resolve(subtitles.getTemplateName()).toFile());
        Map<String, Object> root = new HashMap<>();
        Map<String, String> mx = new HashMap<>();
        List<String> list = Splitter.on(",").splitToList(subtitles.getSentence());
        for (int i = 0; i < list.size(); i++) {
            mx.put("sentences" + i, list.get(i));
        }
        root.put("mx", mx);
        Template temp = cfg.getTemplate("template.ftl");
        try (FileWriter writer = new FileWriter(path)) {
            temp.process(root, writer);
        } catch (Exception e) {
            logger.error("生成ass文件报错", e);
        }
        return path.toString();
    }


}
