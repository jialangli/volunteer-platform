package com.gdupt.volunteer.common;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 志愿服务证明 PDF 生成（OpenPDF + Windows 中文字体嵌入）。
 * 优先使用宋体（simsun.ttc），缺失时回退微软雅黑（msyh.ttc）。
 */
@Component
public class PdfService {

    /** 已探测可用的中文字体（Windows 常见路径，含 .ttc 索引 0） */
    private static final String[] FONT_CANDIDATES = {
            "C:/Windows/Fonts/simsun.ttc,0",
            "C:/Windows/Fonts/simhei.ttf",
            "C:/Windows/Fonts/msyh.ttc,0"
    };

    private static BaseFont CJK_FONT = null;

    /** 证明数据载体 */
    public record CertData(String serialNo, String realName, String studentNo,
                           String periodLabel, String totalHours, String issueDate, String orgName) {
    }

    /** 生成一页 A4 志愿服务证明 PDF */
    public byte[] generateCert(CertData data) throws DocumentException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 60, 60, 60, 60);
        PdfWriter.getInstance(doc, out);
        doc.open();

        BaseFont bf = cjkFont();
        Font titleFont = new Font(bf, 28, Font.BOLD);
        Font subFont = new Font(bf, 12, Font.NORMAL);
        Font labelFont = new Font(bf, 14, Font.NORMAL);
        Font bodyFont = new Font(bf, 13, Font.NORMAL);
        Font footerFont = new Font(bf, 11, Font.NORMAL);

        Paragraph title = new Paragraph("志愿服务证明", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(8);
        doc.add(title);

        Paragraph serial = new Paragraph("编号：" + data.serialNo(), subFont);
        serial.setAlignment(Element.ALIGN_CENTER);
        serial.setSpacingAfter(28);
        doc.add(serial);

        String body = "兹证明 " + data.realName() + "（学号 " + data.studentNo() + "）同学于 "
                + data.periodLabel() + " 期间，积极参加志愿服务活动，"
                + "累计服务时长 " + data.totalHours() + " 小时，表现良好，特此证明。";
        Paragraph text = new Paragraph(body, bodyFont);
        text.setFirstLineIndent(26);
        text.setSpacingAfter(16);
        text.setLeading(26);
        doc.add(text);

        Paragraph period = new Paragraph("证明周期：" + data.periodLabel(), labelFont);
        period.setSpacingAfter(6);
        doc.add(period);
        Paragraph hours = new Paragraph("累计服务时长：" + data.totalHours() + " 小时", labelFont);
        hours.setSpacingAfter(40);
        doc.add(hours);

        Paragraph sign = new Paragraph("签发单位：" + data.orgName(), footerFont);
        sign.setAlignment(Element.ALIGN_RIGHT);
        sign.setSpacingAfter(4);
        doc.add(sign);
        Paragraph date = new Paragraph(data.issueDate(), footerFont);
        date.setAlignment(Element.ALIGN_RIGHT);
        doc.add(date);

        Paragraph tip = new Paragraph("本证明电子版与加盖公章纸质版具有同等效力，可通过编号在平台核验。", footerFont);
        tip.setSpacingBefore(60);
        tip.setAlignment(Element.ALIGN_CENTER);
        doc.add(tip);

        doc.close();
        return out.toByteArray();
    }

    /** 探测并缓存中文字体（失败时抛明确异常，由全局异常处理兜底） */
    private static synchronized BaseFont cjkFont() throws DocumentException, IOException {
        if (CJK_FONT != null) {
            return CJK_FONT;
        }
        DocumentException last = null;
        for (String candidate : FONT_CANDIDATES) {
            Path p = Paths.get(candidate.split(",")[0]);
            if (!Files.exists(p)) {
                continue;
            }
            try {
                CJK_FONT = BaseFont.createFont(candidate, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                return CJK_FONT;
            } catch (DocumentException e) {
                last = e;
            }
        }
        DocumentException ex = new DocumentException("未找到可用中文字体（系统缺少宋体/黑体/微软雅黑），证明 PDF 无法生成");
        if (last != null) {
            ex.initCause(last);
        }
        throw ex;
    }

    /** 当前日期文本（yyyy年MM月dd日） */
    public static String todayText() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日", Locale.CHINA));
    }
}
