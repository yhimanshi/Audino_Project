package com.audino;

import com.audino.model.Medication;
import com.audino.model.Patient;
import com.audino.model.PrescribedDrug;
import com.audino.model.Prescription;
import com.audino.model.TabletMedication;
import org.junit.jupiter.api.Test;
import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PdfGenerationTest {

    @Test
    public void testPdfGeneration() {
        try {
            Patient patient = new Patient();
            patient.setPatientId("PAT-123");
            patient.setFirstName("John");
            patient.setLastName("Doe");

            Prescription prescription = new Prescription(patient, "Dr. Smith");
            TabletMedication med = new TabletMedication();
            med.setGenericName("Aspirin");
            med.setPricePerUnit(10.0);
            PrescribedDrug drug = new PrescribedDrug(med, "1", "Daily", "7 days", "", "Dr. Smith");
            drug.calculateCost();
            prescription.addPrescribedDrug(drug);

            File file = new File("test_prescription.pdf");

            com.itextpdf.text.Document document = new com.itextpdf.text.Document();
            com.itextpdf.text.pdf.PdfWriter.getInstance(document, new java.io.FileOutputStream(file));
            document.open();

            try {
                // Simulate resource loading for test
                java.net.URL logoUrl = new File("src/main/resources/images/AudinoLogo.jpg").toURI().toURL();
                com.itextpdf.text.pdf.PdfPTable headerTable = new com.itextpdf.text.pdf.PdfPTable(2);
                headerTable.setWidthPercentage(100);
                headerTable.setWidths(new float[]{80f, 20f});

                com.itextpdf.text.pdf.PdfPCell emptyCell = new com.itextpdf.text.pdf.PdfPCell();
                emptyCell.setBorder(com.itextpdf.text.pdf.PdfPCell.NO_BORDER);
                headerTable.addCell(emptyCell);

                com.itextpdf.text.pdf.PdfPTable brandTable = new com.itextpdf.text.pdf.PdfPTable(2);
                if (logoUrl != null) {
                    com.itextpdf.text.Image logo = com.itextpdf.text.Image.getInstance(logoUrl);
                    logo.scaleToFit(28, 40);
                    com.itextpdf.text.pdf.PdfPCell logoCell = new com.itextpdf.text.pdf.PdfPCell(logo);
                    logoCell.setBorder(com.itextpdf.text.pdf.PdfPCell.NO_BORDER);
                    logoCell.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_MIDDLE);
                    logoCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
                    brandTable.addCell(logoCell);
                }
                com.itextpdf.text.Font brandFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 14, com.itextpdf.text.Font.BOLD);
                com.itextpdf.text.pdf.PdfPCell nameCell = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase("Audino", brandFont));
                nameCell.setBorder(com.itextpdf.text.pdf.PdfPCell.NO_BORDER);
                nameCell.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_MIDDLE);
                brandTable.addCell(nameCell);

                com.itextpdf.text.pdf.PdfPCell brandCell = new com.itextpdf.text.pdf.PdfPCell(brandTable);
                brandCell.setBorder(com.itextpdf.text.pdf.PdfPCell.NO_BORDER);
                brandCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
                headerTable.addCell(brandCell);

                document.add(headerTable);
            } catch (Exception e) {
                System.err.println("Could not add logo to PDF: " + e.getMessage());
            }

            document.add(new com.itextpdf.text.Paragraph(" "));
            com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 18, com.itextpdf.text.Font.BOLD);
            com.itextpdf.text.Paragraph title = new com.itextpdf.text.Paragraph("Prescription:", titleFont);
            title.setAlignment(com.itextpdf.text.Element.ALIGN_LEFT);
            document.add(title);
            document.add(new com.itextpdf.text.Paragraph(" "));

            document.add(new com.itextpdf.text.Paragraph("Patient Name: " + patient.getFullName()));
            document.add(new com.itextpdf.text.Paragraph("Patient ID: " + patient.getPatientId()));
            document.add(new com.itextpdf.text.Paragraph("Date: " + java.time.LocalDate.now().toString()));
            document.add(new com.itextpdf.text.Paragraph(" "));

            com.itextpdf.text.pdf.PdfPTable table = new com.itextpdf.text.pdf.PdfPTable(5);
            table.setWidthPercentage(100);
            table.addCell("Medication");
            table.addCell("Dosage");
            table.addCell("Frequency");
            table.addCell("Duration");
            table.addCell("Cost (Rs)");

            for (PrescribedDrug d : prescription.getPrescribedDrugs()) {
                String medName = d.getMedication() != null ? d.getMedication().getGenericName() : d.getMedicationId();
                table.addCell(medName);
                table.addCell(d.getDosage());
                table.addCell(d.getFrequency());
                table.addCell(d.getDuration());
                table.addCell(d.getTotalCost() != null ? String.format("%.2f", d.getTotalCost()) : "N/A");
            }

            document.add(table);
            document.add(new com.itextpdf.text.Paragraph(" "));
            document.add(new com.itextpdf.text.Paragraph(String.format("Total Bill: Rs %.2f", prescription.getTotalBill())));

            document.close();

            assertTrue(file.exists());
            assertTrue(file.length() > 0);
            file.delete();

        } catch (Exception e) {
            e.printStackTrace();
            org.junit.jupiter.api.Assertions.fail("PDF Generation failed: " + e.getMessage());
        }
    }
}
