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

        // Inject print styles before generating PDF
        injectPrintStyles(into: webView)

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

            // Automatically restore page styles after PDF generation
            restorePageAfterPDF(webView: webView)

            return fileURL
        } catch {
            print("❌ iOS: Error saving PDF: \(error)")

            // Always restore page styles even on error
            restorePageAfterPDF(webView: webView)

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

    /// Inject print styles into WebView for PDF generation
    private static func injectPrintStyles(into webView: WKWebView) {
        print("📋 iOS: Preparing page for PDF generation (preserving existing styles)")

        let printStylesCSS = """
            console.log('📄 iOS: Preparing page for PDF generation');

            // Simply hide elements marked with .hidden-print class
            // This respects the app's existing print CSS without overriding styles
            const elementsToHide = document.querySelectorAll('.hidden-print');
            console.log('📄 iOS: Found ' + elementsToHide.length + ' elements to hide for PDF');

            elementsToHide.forEach((el, index) => {
                console.log('📄 iOS: Hiding element ' + (index + 1) + ':', el.tagName, el.className);
                el.style.setProperty('display', 'none', 'important');
            });

            // Ensure body is visible and properly styled for PDF
            if (document.body) {
                // Only apply minimal styling if body is completely hidden
                const bodyStyle = window.getComputedStyle(document.body);
                if (bodyStyle.display === 'none' || bodyStyle.visibility === 'hidden') {
                    console.log('⚠️ iOS: Body was hidden, making it visible for PDF');
                    document.body.style.setProperty('display', 'block', 'important');
                    document.body.style.setProperty('visibility', 'visible', 'important');
                }

                // Ensure content is visible with basic print-friendly styling
                document.body.style.setProperty('background-color', '#ffffff', 'important');
                document.body.style.setProperty('color', '#000000', 'important');

                console.log('✅ iOS: Body styling applied for PDF generation');
            }

            // Force any dark mode elements to be visible in PDF
            const darkElements = document.querySelectorAll('[class*="dark:"], .dark');
            darkElements.forEach(el => {
                const computedStyle = window.getComputedStyle(el);
                if (computedStyle.color === 'rgb(255, 255, 255)' || computedStyle.color === 'white') {
                    el.style.setProperty('color', '#000000', 'important');
                }
                if (computedStyle.backgroundColor === 'rgb(0, 0, 0)' || computedStyle.backgroundColor.includes('gray')) {
                    el.style.setProperty('background-color', '#ffffff', 'important');
                }
            });

            console.log('✅ iOS: Page prepared for PDF generation - existing styles preserved');
        """

        webView.evaluateJavaScript(printStylesCSS) { (result, error) in
            if let error = error {
                print("❌ iOS: Error injecting print styles: \(error)")
            } else {
                print("✅ iOS: Print styles injected successfully")
            }
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
    /// Restore page to normal view after PDF generation
    static func restorePageAfterPDF(webView: WKWebView) {
        print("🔄 iOS: Restoring page after PDF generation")

        let restoreJS = """
            console.log('🔄 iOS: Restoring page after PDF generation');

            // Restore .hidden-print elements to visible
            const hiddenPrintElements = document.querySelectorAll('.hidden-print');
            console.log('🔄 iOS: Restoring ' + hiddenPrintElements.length + ' .hidden-print elements');
            hiddenPrintElements.forEach((el, index) => {
                el.style.removeProperty('display');
                console.log('🔄 iOS: Restored element ' + (index + 1) + ':', el.tagName, el.className);
            });

            // Remove PDF-specific inline styles that were added
            const allElements = document.querySelectorAll('*');
            allElements.forEach(el => {
                // Only remove styles that we specifically added during PDF generation
                const style = el.style;

                // Remove forced white background if it was added for PDF
                if (style.backgroundColor === 'rgb(255, 255, 255)' && style.getPropertyPriority('background-color') === 'important') {
                    el.style.removeProperty('background-color');
                }

                // Remove forced black text if it was added for PDF
                if (style.color === 'rgb(0, 0, 0)' && style.getPropertyPriority('color') === 'important') {
                    el.style.removeProperty('color');
                }

                // Remove forced display/visibility if it was added for PDF
                if (style.getPropertyPriority('display') === 'important') {
                    el.style.removeProperty('display');
                }
                if (style.getPropertyPriority('visibility') === 'important') {
                    el.style.removeProperty('visibility');
                }
            });

            console.log('✅ iOS: Page styles restored successfully');
        """

        webView.evaluateJavaScript(restoreJS) { (result, error) in
            if let error = error {
                print("❌ iOS: Error restoring page: \(error)")
            } else {
                print("✅ iOS: Page styles restored successfully")
            }
        }
    }
}
