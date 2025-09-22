package com.vetcalculators.plugins.pdfshare;

import android.content.Context;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.pdf.PrintedPdfDocument;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.util.Log;

import com.getcapacitor.PluginCall;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;

public class PdfPrintDocumentAdapter extends PrintDocumentAdapter {
    private static final String TAG = "PdfPrintAdapter";

    private PrintDocumentAdapter originalAdapter;
    private File outputFile;
    private PluginCall pluginCall;
    private Context context;
    private PrintedPdfDocument pdfDocument;

    public PdfPrintDocumentAdapter(PrintDocumentAdapter originalAdapter, File outputFile, PluginCall pluginCall, Context context) {
        this.originalAdapter = originalAdapter;
        this.outputFile = outputFile;
        this.pluginCall = pluginCall;
        this.context = context;
    }

    @Override
    public void onStart() {
        Log.d(TAG, "PDF generation started");
        originalAdapter.onStart();
    }

    @Override
    public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                        CancellationSignal cancellationSignal, LayoutResultCallback callback,
                        Bundle extras) {

        Log.d(TAG, "onLayout called");

        if (cancellationSignal.isCanceled()) {
            callback.onLayoutCancelled();
            return;
        }

        // Create a new document with the new attributes
        pdfDocument = new PrintedPdfDocument(context, newAttributes);

        // Build document info
        PrintDocumentInfo.Builder builder = new PrintDocumentInfo.Builder(outputFile.getName())
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1); // Assume single page for now

        PrintDocumentInfo info = builder.build();
        callback.onLayoutFinished(info, true);
    }

    @Override
    public void onWrite(PageRange[] pages, ParcelFileDescriptor destination,
                       CancellationSignal cancellationSignal, WriteResultCallback callback) {

        Log.d(TAG, "onWrite called");

        try {
            if (cancellationSignal.isCanceled()) {
                callback.onWriteCancelled();
                return;
            }

            // Start a page
            PdfDocument.Page page = pdfDocument.startPage(0);

            if (cancellationSignal.isCanceled()) {
                callback.onWriteCancelled();
                pdfDocument.close();
                return;
            }

            // Get the canvas to draw on
            Canvas canvas = page.getCanvas();

            // This is where we would render the WebView content
            // For now, we'll use the original adapter's approach

            // Finish the page
            pdfDocument.finishPage(page);

            // Write the document to both the system destination and our file
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                pdfDocument.writeTo(fos);
                fos.flush();

                // Also write to the system destination
                try (FileOutputStream sysFos = new FileOutputStream(destination.getFileDescriptor())) {
                    pdfDocument.writeTo(sysFos);
                    sysFos.flush();
                }

                Log.d(TAG, "PDF written successfully to: " + outputFile.getAbsolutePath());

                // Close the document
                pdfDocument.close();

                // Notify success
                callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});

                // Share the file
                PdfShare.shareFile(outputFile, context, pluginCall);

            } catch (IOException e) {
                Log.e(TAG, "Error writing PDF", e);
                callback.onWriteFailed(e.toString());
                pluginCall.reject("Error writing PDF: " + e.getMessage());
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in onWrite", e);
            callback.onWriteFailed(e.toString());
            pluginCall.reject("Error generating PDF: " + e.getMessage());
        }
    }

    @Override
    public void onFinish() {
        Log.d(TAG, "PDF generation finished");
        originalAdapter.onFinish();
        if (pdfDocument != null) {
            pdfDocument.close();
        }
    }
}