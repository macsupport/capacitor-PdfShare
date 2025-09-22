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
}