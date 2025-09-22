# PDF Share Plugin - Usage Guide

## Integration with VetDrugs App

This guide shows how to integrate the PDF Share plugin specifically with your VetDrugs veterinary calculator app.

## Setup in Your App

### 1. Install the Plugin
```bash
cd /Users/macsupport/Downloads/VETCALCULATORS-APP/VETDrugs
npm install ./cap-pdf/capacitor-plugin-pdfshare
npx cap sync
```

### 2. Import in Your Components
```typescript
// In your Svelte components or JavaScript files
import { PdfShare } from 'capacitor-plugin-pdfshare';
```

## VetDrugs-Specific Usage Examples

### Generate Dosage Calculator PDF
```typescript
// Generate PDF of current dosage calculation
async function exportDosageCalculation() {
  try {
    const result = await PdfShare.generateAndShare({
      elementId: 'printPage',           // Your print container element
      filename: 'veterinary-dosage',    // Default filename
      title: 'Veterinary Dosage Calculator',
      format: 'a4',                     // Professional A4 format
      orientation: 'portrait',
      margins: {
        top: 20,
        bottom: 20,
        left: 20,
        right: 20
      },
      includeBackground: true,          // Include your app styling
      quality: 0.98                     // High quality for professional use
    });

    if (result.success) {
      console.log('✅ PDF shared successfully');
      // Optional: Show success message to user
    } else {
      console.error('❌ PDF export failed:', result.error);
      // Show error message to user
    }
  } catch (error) {
    console.error('❌ Unexpected error:', error);
  }
}
```

### Generate Report for Client Records
```typescript
// Generate PDF for client records without immediate sharing
async function generateClientRecord() {
  const result = await PdfShare.generatePdfOnly({
    elementId: 'dosage-summary',
    filename: `client-record-${Date.now()}`,
    title: 'Veterinary Dosage Record',
    format: 'letter',                   // US standard for client records
    orientation: 'portrait'
  });

  if (result.success) {
    // Store the file path for later use
    localStorage.setItem('lastGeneratedPDF', result.path);

    // Optionally share later
    await PdfShare.shareExistingPdf({
      path: result.path,
      title: 'Veterinary Dosage Record'
    });
  }
}
```

### Landscape Format for Detailed Calculations
```typescript
// For complex multi-drug calculations that need more horizontal space
async function exportDetailedCalculation() {
  await PdfShare.generateAndShare({
    elementId: 'detailed-calculation',
    filename: 'detailed-drug-analysis',
    title: 'Detailed Drug Calculation Analysis',
    format: 'letter',
    orientation: 'landscape',           // Better for wide tables/charts
    margins: {
      top: 15,
      bottom: 15,
      left: 30,
      right: 30
    },
    scale: 1.5                          // Higher resolution for detailed content
  });
}
```

## Integration with Your UI Components

### Svelte Component Example
```svelte
<!-- PDFExportButton.svelte -->
<script>
  import { PdfShare } from 'capacitor-plugin-pdfshare';

  export let calculationData;
  export let patientInfo;

  let isExporting = false;

  async function handleExport() {
    isExporting = true;

    try {
      // Ensure the print page is populated with current data
      // (your existing logic here)

      const result = await PdfShare.generateAndShare({
        elementId: 'printPage',
        filename: `dosage-${patientInfo.name || 'calculation'}`,
        title: `Veterinary Dosage - ${patientInfo.name || 'Patient'}`,
        format: 'a4',
        orientation: 'portrait',
        includeBackground: true
      });

      if (!result.success) {
        // Handle error - show toast/alert to user
        console.error('Export failed:', result.error);
      }
    } catch (error) {
      console.error('Export error:', error);
    } finally {
      isExporting = false;
    }
  }
</script>

<button
  class="btn btn-primary"
  on:click={handleExport}
  disabled={isExporting}
>
  {#if isExporting}
    <span class="loading loading-spinner loading-sm"></span>
    Generating PDF...
  {:else}
    📄 Export PDF
  {/if}
</button>
```

### Framework7 Integration
```javascript
// If using Framework7 components
f7.button.create({
  text: 'Export PDF',
  fill: true,
  on: {
    click: async function() {
      await PdfShare.generateAndShare({
        elementId: 'printPage',
        filename: 'veterinary-dosage',
        title: 'Veterinary Dosage Calculator'
      });
    }
  }
});
```

## Error Handling Best Practices

```typescript
async function safePDFExport() {
  try {
    // Validate that print content exists
    const printElement = document.getElementById('printPage');
    if (!printElement || !printElement.innerHTML.trim()) {
      throw new Error('No content available to export');
    }

    const result = await PdfShare.generateAndShare({
      elementId: 'printPage',
      filename: 'veterinary-dosage',
      title: 'Veterinary Dosage Calculator'
    });

    if (!result.success) {
      // Show user-friendly error message
      showErrorToast(`Export failed: ${result.error}`);
      return;
    }

    // Show success message
    showSuccessToast('PDF exported successfully!');

  } catch (error) {
    console.error('PDF Export Error:', error);
    showErrorToast('Unable to export PDF. Please try again.');
  }
}

function showErrorToast(message) {
  // Your app's toast/notification system
  console.error(message);
}

function showSuccessToast(message) {
  // Your app's success notification system
  console.log(message);
}
```

## CSS Considerations for PDF Output

Make sure your print styles are optimized:

```css
/* Add to your CSS for better PDF output */
@media print {
  #printPage {
    background: white !important;
    color: black !important;
    font-size: 12pt;
    line-height: 1.4;
  }

  /* Hide elements that shouldn't appear in PDF */
  .no-print {
    display: none !important;
  }

  /* Ensure tables don't break across pages */
  table {
    page-break-inside: avoid;
  }

  /* Better spacing for printed content */
  .calculation-row {
    margin-bottom: 1em;
    page-break-inside: avoid;
  }
}
```

## Testing Your Integration

1. **Test on all platforms** your app supports (iOS, Android, Web)
2. **Verify content** appears correctly in the generated PDF
3. **Test error scenarios** (no internet, no content, etc.)
4. **Check file cleanup** - temporary files should be automatically cleaned up
5. **Validate sharing** works with common apps (email, cloud storage, etc.)

## Performance Tips

- **Minimize content** in the `printPage` element for faster generation
- **Use appropriate quality settings** - 0.98 for professional docs, 0.8 for quick previews
- **Cache results** when generating multiple PDFs of the same content
- **Show loading indicators** for better user experience

This integration will give your VetDrugs app professional PDF export capabilities that work seamlessly across all platforms!