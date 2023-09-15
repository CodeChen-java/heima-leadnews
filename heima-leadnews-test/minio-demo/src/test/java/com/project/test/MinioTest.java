//package com.project.test;
//
//import com.heima.file.service.FileStorageService;
//import com.project.MinioApplication;
//import io.minio.MinioClient;
//import io.minio.PutObjectArgs;
//import io.minio.errors.*;
//import org.junit.runner.RunWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.junit4.SpringRunner;
//
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import java.security.InvalidKeyException;
//import java.security.NoSuchAlgorithmException;
//@SpringBootTest(classes = MinioApplication.class)
//@RunWith(SpringRunner.class)
//public class MinioTest {
//    //把HTML文件上传到minio中 并可以在浏览器访问
//    //public static void main(String[] args) throws IOException, ServerException, InvalidBucketNameException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
//    //    FileInputStream fileInputStream = new FileInputStream("D:\\list.html");
//    //    //获取minio链接信息 创建minio客户端
//    //    MinioClient minioClient = MinioClient.builder().credentials("minio", "minio123").endpoint("http://192.168.27.128:9090")
//    //            .build();
//    //    //上传
//    //    PutObjectArgs putObjectArgs = PutObjectArgs.builder()
//    //            .object("list.html")//文件名
//    //            .contentType("text/html")//文件类型
//    //            .bucket("leadnews")//桶名词  与minio创建的名词一致
//    //            .stream(fileInputStream, fileInputStream.available(), -1) //文件流
//    //            .build();
//    //    minioClient.putObject(putObjectArgs);
//    //}
//
//
//    @Autowired
//    private FileStorageService fileStorageService;
//
//    public void test() throws FileNotFoundException {
//        FileInputStream fileInputStream = new FileInputStream("D:\\list.html");
//        fileStorageService.uploadHtmlFile("", "list.html", fileInputStream);
//    }
//}
