import Foundation
import UIKit
import WebKit

class PdfShare {
    static func generatePDF(from webView: WKWebView) -> URL? {
        // Configure print info
        let printInfo = UIPrintInfo(dictionary: nil)
        printInfo.jobName = "Veterinary Dosage"
        printInfo.outputType = .grayscale
        
        // Create the temp file path for PDF
        let tempDir = FileManager.default.temporaryDirectory
        let fileName = "dosage.pdf"
        let fileURL = tempDir.appendingPathComponent(fileName)
        
        // Get the formatter from webview
        let printFormatter = webView.viewPrintFormatter()
        
        // Generate PDF data
        let renderer = UIPrintPageRenderer()
        renderer.addPrintFormatter(printFormatter, startingAtPageAt: 0)
        
        // Configure page size (A4 in points)
        let pageSize = CGRect(x: 0, y: 0, width: 595.2, height: 841.8)
        let printable = pageSize.insetBy(dx: 20, dy: 20)
        
        renderer.setValue(NSValue(cgRect: pageSize), forKey: "paperRect")
        renderer.setValue(NSValue(cgRect: printable), forKey: "printableRect")
        
        // Create PDF
        let pdfData = NSMutableData()
        UIGraphicsBeginPDFContextToData(pdfData, pageSize, nil)
        
        for i in 0..<renderer.numberOfPages {
            UIGraphicsBeginPDFPage()
            renderer.drawPage(at: i, in: UIGraphicsGetPDFContextBounds())
        }
        
        UIGraphicsEndPDFContext()
        
        // Save PDF to temp file
        do {
            try pdfData.write(to: fileURL, options: .atomic)
            return fileURL
        } catch {
            print("Error saving PDF: \(error)")
            return nil
        }
    }
    
    static func presentShareSheet(for fileURL: URL, from viewController: UIViewController, completion: (() -> Void)? = nil) {
        let activityViewController = UIActivityViewController(
            activityItems: [fileURL],
            applicationActivities: nil
        )
        
        // For iPad
        if let popover = activityViewController.popoverPresentationController {
            popover.sourceView = viewController.view
            popover.sourceRect = CGRect(x: viewController.view.bounds.midX,
                                      y: viewController.view.bounds.midY,
                                      width: 0,
                                      height: 0)
            popover.permittedArrowDirections = []
        }
        
        viewController.present(activityViewController, animated: true, completion: completion)
    }
}
