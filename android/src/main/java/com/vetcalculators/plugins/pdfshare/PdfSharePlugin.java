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
            WebView webView = null;
            try {
                Log.d(TAG, "🔧 Android: Starting PDF generation");

                // Get options from call
                JSObject options = call.getData();
                String filename = options.optString("filename", "veterinary-dosage");
                String title = options.optString("title", "Veterinary Dosage");

                webView = bridge.getWebView();
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

                // Automatically restore page after PDF generation
                restorePageStyles(webView);

            } catch (Exception e) {
                Log.e(TAG, "❌ Error generating PDF", e);
                // Always restore page styles even on error
                restorePageStyles(webView);
                call.reject("Error generating PDF: " + e.getMessage());
            }
        });
    }

    @PluginMethod
    public void generatePdfOnly(PluginCall call) {
        call.setKeepAlive(true);

        new Handler(Looper.getMainLooper()).post(() -> {
            WebView webView = null;
            try {
                Log.d(TAG, "🔧 Android: Generating PDF without sharing");

                JSObject options = call.getData();
                String filename = options.optString("filename", "veterinary-dosage");

                webView = bridge.getWebView();
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
                    // Restore page styles even on IO error
                    restorePageStyles(webView);
                    call.reject("Error writing PDF: " + e.getMessage());
                } finally {
                    document.close();
                    // Always restore page styles when done
                    restorePageStyles(webView);
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ Error generating PDF", e);
                // Always restore page styles even on error
                restorePageStyles(webView);
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
     * Prepare page for PDF generation without overriding existing styles
     */
    private void injectPrintStyles(WebView webView) {
        Log.d(TAG, "📋 Android: Preparing page for PDF generation (preserving existing styles)");

        String printStylesJS =
            "console.log('📄 Android: Preparing page for PDF generation');\n" +
            "\n" +
            "// Simply hide elements marked with .hidden-print class\n" +
            "// This respects the app's existing print CSS without overriding styles\n" +
            "const elementsToHide = document.querySelectorAll('.hidden-print');\n" +
            "console.log('📄 Android: Found ' + elementsToHide.length + ' elements to hide for PDF');\n" +
            "\n" +
            "elementsToHide.forEach((el, index) => {\n" +
            "    console.log('📄 Android: Hiding element ' + (index + 1) + ':', el.tagName, el.className);\n" +
            "    el.style.setProperty('display', 'none', 'important');\n" +
            "});\n" +
            "\n" +
            "// Ensure body is visible and properly styled for PDF\n" +
            "if (document.body) {\n" +
            "    // Only apply minimal styling if body is completely hidden\n" +
            "    const bodyStyle = window.getComputedStyle(document.body);\n" +
            "    if (bodyStyle.display === 'none' || bodyStyle.visibility === 'hidden') {\n" +
            "        console.log('⚠️ Android: Body was hidden, making it visible for PDF');\n" +
            "        document.body.style.setProperty('display', 'block', 'important');\n" +
            "        document.body.style.setProperty('visibility', 'visible', 'important');\n" +
            "    }\n" +
            "\n" +
            "    // Ensure content is visible with basic print-friendly styling\n" +
            "    document.body.style.setProperty('background-color', '#ffffff', 'important');\n" +
            "    document.body.style.setProperty('color', '#000000', 'important');\n" +
            "\n" +
            "    console.log('✅ Android: Body styling applied for PDF generation');\n" +
            "}\n" +
            "\n" +
            "// Force any dark mode elements to be visible in PDF\n" +
            "const darkElements = document.querySelectorAll('[class*=\"dark:\"], .dark');\n" +
            "darkElements.forEach(el => {\n" +
            "    const computedStyle = window.getComputedStyle(el);\n" +
            "    if (computedStyle.color === 'rgb(255, 255, 255)' || computedStyle.color === 'white') {\n" +
            "        el.style.setProperty('color', '#000000', 'important');\n" +
            "    }\n" +
            "    if (computedStyle.backgroundColor === 'rgb(0, 0, 0)' || computedStyle.backgroundColor.includes('gray')) {\n" +
            "        el.style.setProperty('background-color', '#ffffff', 'important');\n" +
            "    }\n" +
            "});\n" +
            "\n" +
            "console.log('✅ Android: Page prepared for PDF generation - existing styles preserved');\n";

        webView.evaluateJavascript(printStylesJS, result -> {
            if (result != null) {
                Log.d(TAG, "✅ Android: Print styles injected successfully");
            } else {
                Log.e(TAG, "❌ Android: Error injecting print styles");
            }
        });
    }

    /**
     * Restore page styles after PDF generation (internal method)
     */
    private void restorePageStyles(WebView webView) {
        if (webView == null) {
            Log.w(TAG, "⚠️ WebView not available for style restoration");
            return;
        }

        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                Log.d(TAG, "🔄 Android: Restoring page styles after PDF generation");

                String restoreJS =
                    "console.log('🔄 Android: Restoring page after PDF generation');\n" +
                    "\n" +
                    "// Restore .hidden-print elements to visible\n" +
                    "const hiddenPrintElements = document.querySelectorAll('.hidden-print');\n" +
                    "console.log('🔄 Android: Restoring ' + hiddenPrintElements.length + ' .hidden-print elements');\n" +
                    "hiddenPrintElements.forEach((el, index) => {\n" +
                    "    el.style.removeProperty('display');\n" +
                    "    console.log('🔄 Android: Restored element ' + (index + 1) + ':', el.tagName, el.className);\n" +
                    "});\n" +
                    "\n" +
                    "// Remove PDF-specific inline styles that were added\n" +
                    "const allElements = document.querySelectorAll('*');\n" +
                    "allElements.forEach(el => {\n" +
                    "    // Only remove styles that we specifically added during PDF generation\n" +
                    "    const style = el.style;\n" +
                    "    \n" +
                    "    // Remove forced white background if it was added for PDF\n" +
                    "    if (style.backgroundColor === 'rgb(255, 255, 255)' && style.getPropertyPriority('background-color') === 'important') {\n" +
                    "        el.style.removeProperty('background-color');\n" +
                    "    }\n" +
                    "    \n" +
                    "    // Remove forced black text if it was added for PDF\n" +
                    "    if (style.color === 'rgb(0, 0, 0)' && style.getPropertyPriority('color') === 'important') {\n" +
                    "        el.style.removeProperty('color');\n" +
                    "    }\n" +
                    "    \n" +
                    "    // Remove forced display/visibility if it was added for PDF\n" +
                    "    if (style.getPropertyPriority('display') === 'important') {\n" +
                    "        el.style.removeProperty('display');\n" +
                    "    }\n" +
                    "    if (style.getPropertyPriority('visibility') === 'important') {\n" +
                    "        el.style.removeProperty('visibility');\n" +
                    "    }\n" +
                    "});\n" +
                    "\n" +
                    "console.log('✅ Android: Page styles restored successfully');\n";

                webView.evaluateJavascript(restoreJS, result -> {
                    Log.d(TAG, "✅ Android: Page styles restored successfully");
                });

            } catch (Exception e) {
                Log.e(TAG, "❌ Error restoring page styles", e);
            }
        });
    }

    /**
     * Restore page to normal view after PDF generation (public method for manual calls)
     */
    @PluginMethod
    public void restorePageAfterPDF(PluginCall call) {
        WebView webView = bridge.getWebView();
        if (webView == null) {
            call.reject("WebView not available");
            return;
        }

        restorePageStyles(webView);
        call.resolve();
    }
}