package com.project.tess4j;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;

import java.io.File;

public class Application {

    /**
     * 识别图片中的文字
     * @param args
     */
    public static void main(String[] args) throws Exception{
        //创建实例
        ITesseract tesseract = new Tesseract();

        //设置字体库路径
        tesseract.setDatapath("E:\\tesseract");
        //设置语言
        tesseract.setLanguage("chi_sim");
        File file = new File("E:\\1.png");
        //识别图片
        String result = tesseract.doOCR(file);
        System.out.println("识别的结果为：" +result.replaceAll("\\r|\\n", "---"));
    }
}
