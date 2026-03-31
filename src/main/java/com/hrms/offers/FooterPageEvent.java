package com.hrms.offers;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

public class FooterPageEvent extends PdfPageEventHelper {

    private Font companyFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
    private Font addressFont = new Font(Font.FontFamily.HELVETICA, 9);

    @Override
    public void onEndPage(PdfWriter writer, Document document) {

        try {
            PdfContentByte canvas = writer.getDirectContent();

            ColumnText ct = new ColumnText(canvas);

            Phrase footer = new Phrase();
            footer.add(new Chunk("Kambson Private Limited.\n", companyFont));
            footer.add(new Chunk(
                    "5030, 5th floor, A, Wing Marvel Fuego, opposite SEASONS MALL, Amanora Park Town, Hadapsar, Pune, Maharashtra 411036\nwww.kambson.com",
                    addressFont
            ));
            float footerY = document.bottom() - 30;

            ct.setSimpleColumn(
                    footer,
                    document.left(),
                    footerY,
                    document.right(),
                    footerY + 30,
                    10,
                    Element.ALIGN_CENTER
            );

//            ct.setSimpleColumn(
//                    footer,
//                    document.left(),
//                    document.bottom() - 20,
//                    document.right(),
//                    document.bottom() + 10,
//                    10,
//                    Element.ALIGN_CENTER
//            );

            ct.go();
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        }
    }
}