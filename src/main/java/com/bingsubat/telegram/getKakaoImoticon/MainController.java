package com.bingsubat.telegram.getKakaoImoticon;


import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

@Controller
@RequestMapping(value = "/BingSuBat")
public class MainController {
    
    @Value("${api.kakaoImoticon}")
    String kakaoApiUrl;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String index() {
        return "index";
    }

    @RequestMapping(value = "/getKakaoImoticon", method = RequestMethod.GET, produces = "application/text; charset=utf8")
    @ResponseBody
    public void apiTest(@RequestParam HashMap<String, Object> paramMap) {
        HashMap<String, Object> resultMap = new HashMap<String, Object>();
        List<String> imageResultList = new ArrayList<String>();
        
        String inputUrl = (String)paramMap.get("url");
        
        if(StringUtils.isNotBlank(inputUrl)) {
            String url = kakaoApiUrl + inputUrl.split("https://e.kakao.com/t/")[1];
            
            JSONObject resultObj = new JSONObject();
            
            try {
                String result = apiGetRequest(url, new HttpHeaders());
                resultObj = (JSONObject)new JSONParser().parse(result);
                
                if(Integer.parseInt(String.valueOf(resultObj.get("status"))) == 0) {
                    resultObj = (JSONObject)resultObj.get("result");
                    
                    resultMap.put("title", resultObj.get("title"));
                    resultMap.put("titleImageUrl", resultObj.get("titleImageUrl"));
                    resultMap.put("thumbnailUrls", resultObj.get("thumbnailUrls"));
                }
            } catch (Exception e) {
                // TODO: handle exception
            }
            
            if(resultMap.containsKey("title") && resultMap.containsKey("thumbnailUrls")) {
                String title = (String)resultMap.get("title");
                List<String> thumbnailUrls = (List<String>)resultMap.get("thumbnailUrls");
                int idx = 1;
                
                for(String thumbnailUrl : thumbnailUrls) {
                    try {
                        imageResultList.add(saveImage(title + "_" + String.valueOf(idx), thumbnailUrl));
                    } catch (Exception e) {
                        // TODO: handle exception
                    }
                    
                    idx++;
                }
            }
        }
    }
    
    private String saveImage(String title, String url) throws MalformedURLException, IOException {
        String result = title + "_" + UUID.randomUUID() + ".png";
        BufferedImage image = ImageIO.read(new URL(url));
        
        int width = image.getWidth();
        int height = image.getHeight();
        
        int newWidth = (int)(width * ((double)512 / (double)width));
        int newHeight = (int)(height * ((double)512 / (double)width));
        
        image = imageToBufferedImageTransparency(image, newWidth, newHeight);
        
        File test = new File(result);
        test.deleteOnExit();
        
        if(ImageIO.write(image, "png", test))
            return result;
        else
            return "";
    }
    
    private static BufferedImage imageToBufferedImageTransparency(Image image, int width, int height) {
        BufferedImage dest = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = dest.createGraphics();
        
        Image resizeImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        
        g2.drawImage(resizeImage, 0, 0, null);
        g2.dispose();
        
        return dest;
    }
    
    private String apiGetRequest(String url, HttpHeaders headers) {
        RestTemplate res = new RestTemplate();
        
        HttpEntity<?> entity = new HttpEntity<>(headers);
        
        ResponseEntity<String> result = res.exchange(url, HttpMethod.GET, entity, String.class);
        
        return result.getBody();
    }
}
