package com.vetcalculators.plugins.pdfshare;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.core.content.FileProvider;

import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;

import java.io.File;

public class PdfShare {
    private static final String TAG = "PdfShare";

    /**
     * Share a PDF file using Android's share intent
     */
    public static void shareFile(File pdfFile, Context context, PluginCall call) {
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                if (!pdfFile.exists()) {
                    Log.e(TAG, "PDF file does not exist: " + pdfFile.getAbsolutePath());
                    call.reject("PDF file not found");
                    return;
                }

                Log.d(TAG, "Sharing PDF file: " + pdfFile.getAbsolutePath());
                Log.d(TAG, "File size: " + pdfFile.length() + " bytes");

                // Get URI for the file using FileProvider
                Uri contentUri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    pdfFile
                );

                // Create share intent
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("application/pdf");
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Veterinary Dosage PDF");
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                // Create chooser
                Intent chooser = Intent.createChooser(shareIntent, "Share PDF");
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                context.startActivity(chooser);

                // Return success
                JSObject ret = new JSObject();
                ret.put("success", true);
                ret.put("path", pdfFile.getAbsolutePath());
                call.resolve(ret);

                Log.d(TAG, "PDF shared successfully");

            } catch (Exception e) {
                Log.e(TAG, "Error sharing PDF", e);
                call.reject("Error sharing PDF: " + e.getMessage());
            }
        });
    }

    /**
     * Generate file name with timestamp
     */
    public static String generateFileName(String baseFileName) {
        if (baseFileName == null || baseFileName.isEmpty()) {
            baseFileName = "veterinary-dosage";
        }
        return baseFileName + "_" + System.currentTimeMillis() + ".pdf";
    }

    /**
     * Clean up old PDF files to prevent storage bloat
     */
    public static void cleanupOldFiles(Context context) {
        try {
            File cacheDir = context.getCacheDir();
            File[] files = cacheDir.listFiles((dir, name) -> name.endsWith(".pdf"));

            if (files != null) {
                long currentTime = System.currentTimeMillis();
                int deletedCount = 0;

                for (File file : files) {
                    // Delete files older than 1 hour
                    if (currentTime - file.lastModified() > 3600000) {
                        if (file.delete()) {
                            deletedCount++;
                        }
                    }
                }

                if (deletedCount > 0) {
                    Log.d(TAG, "Cleaned up " + deletedCount + " old PDF files");
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error cleaning up old files", e);
        }
    }

    public String echo(String value) {
        Log.i("Echo", value);
        return value;
    }
}
