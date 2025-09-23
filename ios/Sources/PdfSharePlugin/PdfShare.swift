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

    /// Inject print styles into WebView for PDF generation
    private static func injectPrintStyles(into webView: WKWebView) {
        print("📋 iOS: Injecting print styles for PDF generation")

        let printStylesCSS = """
            // Remove existing print styles injection if any
            const existingPrintStyles = document.getElementById('pdf-print-styles');
            if (existingPrintStyles) {
                existingPrintStyles.remove();
            }

            // Create style element for print styles
            const printStyleElement = document.createElement('style');
            printStyleElement.id = 'pdf-print-styles';
            printStyleElement.setAttribute('type', 'text/css');

            // Extract print styles from app-min.css and tailwind.css
            let printStyles = '';

            // Get all stylesheets
            const stylesheets = Array.from(document.styleSheets);
            console.log('📄 iOS: Found ' + stylesheets.length + ' stylesheets');

            for (const stylesheet of stylesheets) {
                try {
                    const href = stylesheet.href ? new URL(stylesheet.href).pathname : 'inline';

                    // Only process app-min.css and tailwind.css
                    if (href.includes('app-min.css') || href.includes('tailwind.css') || !stylesheet.href) {
                        console.log('📄 iOS: Processing stylesheet: ' + href);

                        const rules = stylesheet.cssRules || stylesheet.rules;
                        if (rules) {
                            for (let i = 0; i < rules.length; i++) {
                                const rule = rules[i];

                                // Extract @media print rules
                                if (rule.type === CSSRule.MEDIA_RULE && rule.media.mediaText.includes('print')) {
                                    console.log('🎯 iOS: Found print media rule in ' + href);

                                    // Remove @media print wrapper and apply styles directly
                                    const innerCSS = rule.cssText
                                        .replace(/@media[^{]+\\{/, '')
                                        .replace(/\\}$/, '');

                                    printStyles += '/* From ' + href + ' */\\n' + innerCSS + '\\n\\n';
                                }
                            }
                        }
                    }
                } catch (e) {
                    console.log('❌ iOS: Skipping stylesheet due to CORS: ' + e);
                }
            }

            // Add VetDrugs-specific print defaults if no styles found
            if (!printStyles.trim()) {
                console.log('⚠️ iOS: No print styles found, adding VetDrugs defaults');
                printStyles = `
                    /* VetDrugs PDF Print Styles */
                    body {
                        background: white !important;
                        color: black !important;
                        font-family: Arial, sans-serif !important;
                        font-size: 10pt !important;
                        line-height: 1.2 !important;
                        margin: 0 !important;
                        padding: 15px !important;
                    }

                    .card {
                        margin: 3px 0 !important;
                        padding: 6px !important;
                        border: 1px solid #ddd !important;
                        font-size: 9pt !important;
                        page-break-inside: avoid !important;
                    }

                    .card-content {
                        padding: 4px !important;
                        margin: 0 !important;
                    }

                    h1, h2, h3, h4 {
                        font-size: 11pt !important;
                        font-weight: bold !important;
                        margin: 0 0 4px 0 !important;
                        padding: 0 !important;
                    }

                    .dosage-info, .concentration-info, .drug-details {
                        font-size: 9pt !important;
                        line-height: 1.1 !important;
                        margin: 2px 0 !important;
                    }

                    .navbar, .toolbar, .searchbar, .tab-link,
                    .floating-button, .back-button, .hidden-print,
                    .no-print, button:not(.print-button),
                    .btn:not(.print-button) {
                        display: none !important;
                    }

                    .dark\\\\:bg-gray-800, .dark\\\\:bg-gray-700, .dark\\\\:bg-slate-900 {
                        background-color: white !important;
                    }

                    .dark\\\\:text-white, .dark\\\\:text-gray-300 {
                        color: black !important;
                    }
                `;
            }

            // Apply the print styles
            printStyleElement.textContent = printStyles;
            document.head.appendChild(printStyleElement);

            // Hide elements that shouldn't be in print
            const hideSelectors = [
                '.navbar', '.toolbar', '.searchbar', '.tab-link',
                '.floating-button', '.back-button', '.no-print',
                'button:not(.print-button)', '.btn:not(.print-button)'
            ];

            hideSelectors.forEach(selector => {
                const elements = document.querySelectorAll(selector);
                elements.forEach(el => {
                    el.style.display = 'none';
                });
            });

            // Apply print-friendly body styling
            document.body.style.backgroundColor = '#ffffff';
            document.body.style.color = '#000000';
            document.body.style.fontFamily = 'Arial, sans-serif';

            console.log('✅ iOS: Print styles applied successfully');
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
}
