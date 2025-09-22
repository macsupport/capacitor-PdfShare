import Foundation
import Capacitor
import WebKit

@objc(PdfSharePlugin)
public class PdfSharePlugin: CAPPlugin {

    @objc func generateAndShare(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            print("🔧 iOS: Starting PDF generation and share")

            guard let webView = self.webView else {
                call.reject("WebView not found")
                return
            }

            guard let presentationController = self.bridge?.viewController else {
                call.reject("View controller not found")
                return
            }

            // Parse options
            let options = PdfOptions.from(call: call)

            guard let fileURL = PdfShare.generatePDF(from: webView, options: options) else {
                call.reject("Failed to generate PDF")
                return
            }

            PdfShare.presentShareSheet(for: fileURL, from: presentationController, title: options.title) {
                let result: [String: Any] = [
                    "success": true,
                    "path": fileURL.path
                ]
                call.resolve(result)
            }
        }
    }

    @objc func generatePdfOnly(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            print("🔧 iOS: Generating PDF without sharing")

            guard let webView = self.webView else {
                call.reject("WebView not found")
                return
            }

            let options = PdfOptions.from(call: call)

            guard let fileURL = PdfShare.generatePDF(from: webView, options: options) else {
                call.reject("Failed to generate PDF")
                return
            }

            let result: [String: Any] = [
                "success": true,
                "path": fileURL.path
            ]
            call.resolve(result)
        }
    }

    @objc func shareExistingPdf(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            guard let path = call.getString("path") else {
                call.reject("PDF path is required")
                return
            }

            let fileURL = URL(fileURLWithPath: path)

            guard FileManager.default.fileExists(atPath: path) else {
                call.reject("PDF file not found at path: \(path)")
                return
            }

            guard let presentationController = self.bridge?.viewController else {
                call.reject("View controller not found")
                return
            }

            let title = call.getString("title") ?? "PDF Document"

            PdfShare.presentShareSheet(for: fileURL, from: presentationController, title: title) {
                let result: [String: Any] = ["success": true]
                call.resolve(result)
            }
        }
    }
}
