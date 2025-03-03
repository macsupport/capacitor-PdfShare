import Foundation
import Capacitor
import WebKit

@objc(PdfSharePlugin)
public class PdfSharePlugin: CAPPlugin {
    @objc func generateAndShare(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            guard let webView = self.webView else {
                call.reject("WebView not found")
                return
            }
            
            guard let fileURL = PdfShare.generatePDF(from: webView),
                  let presentationController = self.bridge?.viewController else {
                call.reject("Failed to generate PDF")
                return
            }
            
            PdfShare.presentShareSheet(for: fileURL, from: presentationController) {
                call.resolve()
            }
        }
    }
}
