package com.vetcalculators.plugins.pdfshare;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebView;
import android.content.Context;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@CapacitorPlugin(name = "PdfShare")
public class PdfSharePlugin extends Plugin {
    private static final String TAG = "PdfSharePlugin";

    @PluginMethod
    public void generateAndShare(PluginCall call) {
        call.setKeepAlive(true);

        // Clean up old files first
        PdfShare.cleanupOldFiles(getContext());

        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                Log.d(TAG, "🔧 Android: Starting PDF generation");

                // Get options from call
                JSObject options = call.getData();
                String filename = options.optString("filename", "veterinary-dosage");
                String title = options.optString("title", "Veterinary Dosage");

                WebView webView = bridge.getWebView();
                Context context = getContext();

                if (webView == null) {
                    call.reject("WebView not available");
                    return;
                }

                // Generate unique filename
                String uniqueFilename = PdfShare.generateFileName(filename);
                File pdfFile = new File(context.getCacheDir(), uniqueFilename);

                Log.d(TAG, "📄 Generating PDF: " + pdfFile.getAbsolutePath());

                // Inject print styles before generating PDF
                injectPrintStyles(webView);

                // Create PDF document
                PdfDocument document = new PdfDocument();

                // Get WebView dimensions
                int webViewWidth = webView.getWidth();
                int webViewHeight = webView.getHeight();

                if (webViewWidth <= 0 || webViewHeight <= 0) {
                    Log.w(TAG, "WebView has no dimensions, using defaults");
                    webViewWidth = 800;
                    webViewHeight = 1200;
                }

                // Define A4 page size in points (72 DPI)
                int pageWidth = 595;  // A4 width in points
                int pageHeight = 842; // A4 height in points

                Log.d(TAG, "📐 WebView size: " + webViewWidth + "x" + webViewHeight);
                Log.d(TAG, "📐 PDF page size: " + pageWidth + "x" + pageHeight);

                // Create page info
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                    pageWidth, pageHeight, 1).create();

                // Start page
                PdfDocument.Page page = document.startPage(pageInfo);
                Canvas canvas = page.getCanvas();

                // Scale the WebView to fit the page
                float scaleX = (float) pageWidth / webViewWidth;
                float scaleY = (float) pageHeight / webViewHeight;
                float scale = Math.min(scaleX, scaleY);

                canvas.scale(scale, scale);

                // Draw WebView to canvas
                webView.draw(canvas);

                // Finish page
                document.finishPage(page);

                // Write PDF to file
                try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
                    document.writeTo(fos);
                    Log.d(TAG, "✅ PDF written successfully");
                } catch (IOException e) {
                    Log.e(TAG, "❌ Error writing PDF", e);
                    call.reject("Error writing PDF: " + e.getMessage());
                    return;
                } finally {
                    document.close();
                }

                // Share the file
                PdfShare.shareFile(pdfFile, context, call);

            } catch (Exception e) {
                Log.e(TAG, "❌ Error generating PDF", e);
                call.reject("Error generating PDF: " + e.getMessage());
            }
        });
    }

    @PluginMethod
    public void generatePdfOnly(PluginCall call) {
        call.setKeepAlive(true);

        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                Log.d(TAG, "🔧 Android: Generating PDF without sharing");

                JSObject options = call.getData();
                String filename = options.optString("filename", "veterinary-dosage");

                WebView webView = bridge.getWebView();
                Context context = getContext();

                if (webView == null) {
                    call.reject("WebView not available");
                    return;
                }

                String uniqueFilename = PdfShare.generateFileName(filename);
                File pdfFile = new File(context.getCacheDir(), uniqueFilename);

                // Inject print styles before generating PDF
                injectPrintStyles(webView);

                // Same PDF generation logic as above
                PdfDocument document = new PdfDocument();

                int webViewWidth = webView.getWidth();
                int webViewHeight = webView.getHeight();

                if (webViewWidth <= 0 || webViewHeight <= 0) {
                    webViewWidth = 800;
                    webViewHeight = 1200;
                }

                int pageWidth = 595;
                int pageHeight = 842;

                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                    pageWidth, pageHeight, 1).create();

                PdfDocument.Page page = document.startPage(pageInfo);
                Canvas canvas = page.getCanvas();

                float scaleX = (float) pageWidth / webViewWidth;
                float scaleY = (float) pageHeight / webViewHeight;
                float scale = Math.min(scaleX, scaleY);

                canvas.scale(scale, scale);
                webView.draw(canvas);
                document.finishPage(page);

                try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
                    document.writeTo(fos);

                    JSObject ret = new JSObject();
                    ret.put("success", true);
                    ret.put("path", pdfFile.getAbsolutePath());
                    call.resolve(ret);

                } catch (IOException e) {
                    call.reject("Error writing PDF: " + e.getMessage());
                } finally {
                    document.close();
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ Error generating PDF", e);
                call.reject("Error generating PDF: " + e.getMessage());
            }
        });
    }

    @PluginMethod
    public void shareExistingPdf(PluginCall call) {
        JSObject options = call.getData();
        String path = options.getString("path");

        if (path == null || path.isEmpty()) {
            call.reject("PDF path is required");
            return;
        }

        File pdfFile = new File(path);
        if (!pdfFile.exists()) {
            call.reject("PDF file not found at path: " + path);
            return;
        }

        PdfShare.shareFile(pdfFile, getContext(), call);
    }

    /**
     * Inject print styles into WebView for PDF generation
     */
    private void injectPrintStyles(WebView webView) {
        Log.d(TAG, "📋 Android: Injecting print styles for PDF generation");

        String printStylesJS =
            "// Remove existing print styles injection if any\n" +
            "const existingPrintStyles = document.getElementById('pdf-print-styles');\n" +
            "if (existingPrintStyles) {\n" +
            "    existingPrintStyles.remove();\n" +
            "}\n" +
            "\n" +
            "// Create style element for print styles\n" +
            "const printStyleElement = document.createElement('style');\n" +
            "printStyleElement.id = 'pdf-print-styles';\n" +
            "printStyleElement.setAttribute('type', 'text/css');\n" +
            "\n" +
            "// Extract print styles from app-min.css and tailwind.css\n" +
            "let printStyles = '';\n" +
            "\n" +
            "// Get all stylesheets\n" +
            "const stylesheets = Array.from(document.styleSheets);\n" +
            "console.log('📄 Android: Found ' + stylesheets.length + ' stylesheets');\n" +
            "\n" +
            "for (const stylesheet of stylesheets) {\n" +
            "    try {\n" +
            "        const href = stylesheet.href ? new URL(stylesheet.href).pathname : 'inline';\n" +
            "\n" +
            "        // Only process app-min.css and tailwind.css\n" +
            "        if (href.includes('app-min.css') || href.includes('tailwind.css') || !stylesheet.href) {\n" +
            "            console.log('📄 Android: Processing stylesheet: ' + href);\n" +
            "\n" +
            "            const rules = stylesheet.cssRules || stylesheet.rules;\n" +
            "            if (rules) {\n" +
            "                for (let i = 0; i < rules.length; i++) {\n" +
            "                    const rule = rules[i];\n" +
            "\n" +
            "                    // Extract @media print rules\n" +
            "                    if (rule.type === CSSRule.MEDIA_RULE && rule.media.mediaText.includes('print')) {\n" +
            "                        console.log('🎯 Android: Found print media rule in ' + href);\n" +
            "\n" +
            "                        // Remove @media print wrapper and apply styles directly\n" +
            "                        const innerCSS = rule.cssText\n" +
            "                            .replace(/@media[^{]+\\{/, '')\n" +
            "                            .replace(/\\}$/, '');\n" +
            "\n" +
            "                        printStyles += '/* From ' + href + ' */\\n' + innerCSS + '\\n\\n';\n" +
            "                    }\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "    } catch (e) {\n" +
            "        console.log('❌ Android: Skipping stylesheet due to CORS: ' + e);\n" +
            "    }\n" +
            "}\n" +
            "\n" +
            "// Add minimal fallback if no styles found\n" +
            "if (!printStyles.trim()) {\n" +
            "    console.log('⚠️ Android: No print styles found, adding minimal defaults');\n" +
            "    printStyles = `\n" +
            "        /* Minimal PDF fallback - let app-min.css handle styling */\n" +
            "        body {\n" +
            "            background: white !important;\n" +
            "            color: black !important;\n" +
            "            font-family: Arial, sans-serif !important;\n" +
            "            margin: 0 !important;\n" +
            "            padding: 15px !important;\n" +
            "        }\n" +
            "\n" +
            "        /* Hide navigation and UI elements */\n" +
            "        .navbar, .toolbar, .searchbar, .tab-link,\n" +
            "        .floating-button, .back-button, .hidden-print,\n" +
            "        .no-print, button:not(.print-button),\n" +
            "        .btn:not(.print-button) {\n" +
            "            display: none !important;\n" +
            "        }\n" +
            "\n" +
            "        /* Dark mode overrides */\n" +
            "        .dark\\\\:bg-gray-800, .dark\\\\:bg-gray-700, .dark\\\\:bg-slate-900 {\n" +
            "            background-color: white !important;\n" +
            "        }\n" +
            "\n" +
            "        .dark\\\\:text-white, .dark\\\\:text-gray-300 {\n" +
            "            color: black !important;\n" +
            "        }\n" +
            "    `;\n" +
            "}\n" +
            "\n" +
            "// Apply the print styles\n" +
            "printStyleElement.textContent = printStyles;\n" +
            "document.head.appendChild(printStyleElement);\n" +
            "\n" +
            "// Hide elements that shouldn't be in print\n" +
            "const hideSelectors = [\n" +
            "    '.navbar', '.toolbar', '.searchbar', '.tab-link',\n" +
            "    '.floating-button', '.back-button', '.no-print',\n" +
            "    'button:not(.print-button)', '.btn:not(.print-button)'\n" +
            "];\n" +
            "\n" +
            "hideSelectors.forEach(selector => {\n" +
            "    const elements = document.querySelectorAll(selector);\n" +
            "    elements.forEach(el => {\n" +
            "        el.style.display = 'none';\n" +
            "    });\n" +
            "});\n" +
            "\n" +
            "// Apply print-friendly body styling\n" +
            "document.body.style.backgroundColor = '#ffffff';\n" +
            "document.body.style.color = '#000000';\n" +
            "document.body.style.fontFamily = 'Arial, sans-serif';\n" +
            "\n" +
            "console.log('✅ Android: Print styles applied successfully');\n";

        webView.evaluateJavascript(printStylesJS, result -> {
            if (result != null) {
                Log.d(TAG, "✅ Android: Print styles injected successfully");
            } else {
                Log.e(TAG, "❌ Android: Error injecting print styles");
            }
        });
    }
}