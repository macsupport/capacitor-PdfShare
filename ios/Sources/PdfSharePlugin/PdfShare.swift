import Foundation
import UIKit
import WebKit
import Capacitor

// MARK: - PDF Options Structure

struct PdfOptions {
    let filename: String
    let title: String
    let orientation: PageOrientation
    let format: PageFormat
    let margins: PageMargins
    let outputType: UIPrintInfo.OutputType
    let includeBackground: Bool

    enum PageOrientation {
        case portrait, landscape
    }

    enum PageFormat {
        case a4, letter, legal

        var size: CGSize {
            switch self {
            case .a4:
                return CGSize(width: 595.2, height: 841.8)
            case .letter:
                return CGSize(width: 612, height: 792)
            case .legal:
                return CGSize(width: 612, height: 1008)
            }
        }
    }

    struct PageMargins {
        let top: CGFloat
        let bottom: CGFloat
        let left: CGFloat
        let right: CGFloat

        init(top: CGFloat = 20, bottom: CGFloat = 20, left: CGFloat = 20, right: CGFloat = 20) {
            self.top = top
            self.bottom = bottom
            self.left = left
            self.right = right
        }
    }

    static func from(call: CAPPluginCall) -> PdfOptions {
        let filename = call.getString("filename") ?? "veterinary-dosage"
        let title = call.getString("title") ?? "Veterinary Dosage"

        let orientationString = call.getString("orientation") ?? "portrait"
        let orientation: PageOrientation = orientationString == "landscape" ? .landscape : .portrait

        let formatString = call.getString("format") ?? "a4"
        let format: PageFormat
        switch formatString {
        case "letter": format = .letter
        case "legal": format = .legal
        default: format = .a4
        }

        let marginsObject = call.getObject("margins")
        let margins = PageMargins(
            top: CGFloat(marginsObject?["top"] as? Double ?? 20),
            bottom: CGFloat(marginsObject?["bottom"] as? Double ?? 20),
            left: CGFloat(marginsObject?["left"] as? Double ?? 20),
            right: CGFloat(marginsObject?["right"] as? Double ?? 20)
        )

        let includeBackground = call.getBool("includeBackground") ?? true
        let outputType: UIPrintInfo.OutputType = includeBackground ? .general : .grayscale

        return PdfOptions(
            filename: filename,
            title: title,
            orientation: orientation,
            format: format,
            margins: margins,
            outputType: outputType,
            includeBackground: includeBackground
        )
    }
}

// MARK: - PDF Generation and Sharing

class PdfShare {

    static func generatePDF(from webView: WKWebView, options: PdfOptions = PdfOptions.from(call: CAPPluginCall())) -> URL? {
        print("📄 iOS: Generating PDF with options - \(options.filename), \(options.format), \(options.orientation)")

        // Configure print info
        let printInfo = UIPrintInfo(dictionary: nil)
        printInfo.jobName = options.title
        printInfo.outputType = options.outputType

        // Create unique filename with timestamp
        let timestamp = Int(Date().timeIntervalSince1970)
        let fileName = "\(options.filename)_\(timestamp).pdf"
        let tempDir = FileManager.default.temporaryDirectory
        let fileURL = tempDir.appendingPathComponent(fileName)

        // Clean up old files first
        cleanupOldFiles()

        // Get the formatter from webview
        let printFormatter = webView.viewPrintFormatter()

        // Set up margins
        printFormatter.perPageContentInsets = UIEdgeInsets(
            top: options.margins.top,
            left: options.margins.left,
            bottom: options.margins.bottom,
            right: options.margins.right
        )

        // Generate PDF data
        let renderer = UIPrintPageRenderer()
        renderer.addPrintFormatter(printFormatter, startingAtPageAt: 0)

        // Configure page size based on format and orientation
        var pageSize = options.format.size
        if options.orientation == .landscape {
            pageSize = CGSize(width: pageSize.height, height: pageSize.width)
        }

        let printableRect = CGRect(
            x: options.margins.left,
            y: options.margins.top,
            width: pageSize.width - (options.margins.left + options.margins.right),
            height: pageSize.height - (options.margins.top + options.margins.bottom)
        )

        renderer.setValue(NSValue(cgRect: CGRect(origin: .zero, size: pageSize)), forKey: "paperRect")
        renderer.setValue(NSValue(cgRect: printableRect), forKey: "printableRect")

        // Create PDF
        let pdfData = NSMutableData()

        UIGraphicsBeginPDFContextToData(pdfData, CGRect(origin: .zero, size: pageSize), [
            kCGPDFContextCreator: "VetDrugs PDF Share Plugin",
            kCGPDFContextTitle: options.title
        ])

        let numberOfPages = renderer.numberOfPages
        print("📄 iOS: Rendering \(numberOfPages) pages")

        for i in 0..<numberOfPages {
            UIGraphicsBeginPDFPage()
            let pdfBounds = UIGraphicsGetPDFContextBounds()
            renderer.drawPage(at: i, in: pdfBounds)
        }

        UIGraphicsEndPDFContext()

        // Save PDF to temp file
        do {
            try pdfData.write(to: fileURL, options: .atomic)
            print("✅ iOS: PDF saved successfully to: \(fileURL.path)")
            print("📊 iOS: PDF size: \(pdfData.length) bytes")
            return fileURL
        } catch {
            print("❌ iOS: Error saving PDF: \(error)")
            return nil
        }
    }

    static func presentShareSheet(
        for fileURL: URL,
        from viewController: UIViewController,
        title: String = "PDF Document",
        completion: (() -> Void)? = nil
    ) {
        print("📤 iOS: Presenting share sheet for: \(fileURL.lastPathComponent)")

        let activityViewController = UIActivityViewController(
            activityItems: [fileURL],
            applicationActivities: nil
        )

        // Set subject for email sharing
        activityViewController.setValue(title, forKey: "subject")

        // For iPad - configure popover
        if let popover = activityViewController.popoverPresentationController {
            popover.sourceView = viewController.view
            popover.sourceRect = CGRect(
                x: viewController.view.bounds.midX,
                y: viewController.view.bounds.midY,
                width: 0,
                height: 0
            )
            popover.permittedArrowDirections = []
        }

        // Present the share sheet
        viewController.present(activityViewController, animated: true) {
            print("✅ iOS: Share sheet presented successfully")
            completion?()
        }
    }

    /// Clean up old PDF files to prevent storage bloat
    private static func cleanupOldFiles() {
        let tempDir = FileManager.default.temporaryDirectory
        let fileManager = FileManager.default

        do {
            let files = try fileManager.contentsOfDirectory(at: tempDir, includingPropertiesForKeys: [.contentModificationDateKey], options: [])
            let pdfFiles = files.filter { $0.pathExtension == "pdf" }

            let oneHourAgo = Date().addingTimeInterval(-3600) // 1 hour ago
            var deletedCount = 0

            for fileURL in pdfFiles {
                do {
                    let attributes = try fileManager.attributesOfItem(atPath: fileURL.path)
                    if let modificationDate = attributes[.modificationDate] as? Date,
                       modificationDate < oneHourAgo {
                        try fileManager.removeItem(at: fileURL)
                        deletedCount += 1
                    }
                } catch {
                    print("⚠️ iOS: Error processing file \(fileURL.lastPathComponent): \(error)")
                }
            }

            if deletedCount > 0 {
                print("🧹 iOS: Cleaned up \(deletedCount) old PDF files")
            }

        } catch {
            print("⚠️ iOS: Error cleaning up old files: \(error)")
        }
    }
}
