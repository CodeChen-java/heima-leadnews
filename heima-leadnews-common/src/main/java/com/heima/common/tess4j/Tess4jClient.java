package com.heima.common.tess4j;

import lombok.Getter;
import lombok.Setter;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tess4j")
public class Tess4jClient {
    private String dataPath;
    private String language;

    public String doOCR(BufferedImage image) throws Exception {
        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath(dataPath);
        tesseract.setLanguage(language);
        String result = tesseract.doOCR(image);
        result = result.replaceAll("\\r|\\n", "-").replaceAll(" ","");
        return result;
    }
}
