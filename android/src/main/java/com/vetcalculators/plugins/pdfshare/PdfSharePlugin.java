package com.vetcalculators.plugins.pdfshare;

import android.print.PrintManager;
import android.print.PrintAttributes;
import android.webkit.WebView;
import android.content.Context;
import androidx.core.content.FileProvider;
import android.net.Uri;
import android.content.Intent;
import android.print.PrintJob;
import android.os.Handler;
import android.os.Looper;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.File;

@CapacitorPlugin(name = "PdfShare")
public class PdfSharePlugin extends Plugin {

    @PluginMethod
    public void generateAndShare(PluginCall call) {
        // Save the call for later use
        call.setKeepAlive(true);
        
        // Run WebView operations on main thread
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                WebView webView = bridge.getWebView();
                Context context = getContext();
                
                PrintManager printManager = (PrintManager) context.getSystemService(Context.PRINT_SERVICE);
                
                String jobName = "Veterinary Dosage " + System.currentTimeMillis();
                
                PrintAttributes attributes = new PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setResolution(new PrintAttributes.Resolution("pdf", "pdf", 600, 600))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build();

                // Create a print job
                PrintJob printJob = printManager.print(jobName, 
                    webView.createPrintDocumentAdapter(jobName), 
                    attributes);

                // Monitor print job in background thread
                new Thread(() -> {
                    while (!printJob.isCompleted() && !printJob.isFailed()) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    if (printJob.isCompleted()) {
                        // Get the generated PDF file
                        File pdfFile = new File(context.getCacheDir(), jobName + ".pdf");
                        
                        if (pdfFile.exists()) {
                            try {
                                // Get URI for the file using FileProvider
                                Uri contentUri = FileProvider.getUriForFile(context,
                                    getContext().getPackageName() + ".fileprovider",
                                    pdfFile);

                                // Create share intent
                                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                shareIntent.setType("application/pdf");
                                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                                // Start the share activity on main thread
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    try {
                                        Intent chooser = Intent.createChooser(shareIntent, "Share PDF");
                                        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        context.startActivity(chooser);

                                        JSObject ret = new JSObject();
                                        ret.put("success", true);
                                        call.resolve(ret);
                                    } catch (Exception e) {
                                        call.reject("Error sharing PDF: " + e.getMessage());
                                    }
                                });
                            } catch (Exception e) {
                                call.reject("Error sharing PDF: " + e.getMessage());
                            }
                        } else {
                            call.reject("PDF file not found");
                        }
                    } else {
                        call.reject("Print job failed");
                    }
                }).start();

            } catch (Exception e) {
                call.reject("Error generating PDF: " + e.getMessage());
            }
        });
    }
}