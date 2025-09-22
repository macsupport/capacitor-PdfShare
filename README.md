# capacitor-plugin-pdfshare

Advanced Capacitor plugin for generating and sharing PDFs from WebView content with comprehensive options and cross-platform support.

## Features

- 📱 **Cross-platform**: iOS, Android, and Web support
- 🎨 **Flexible formatting**: Multiple page formats (A4, Letter, Legal) and orientations
- ⚙️ **Configurable options**: Margins, quality, background inclusion, and more
- 🔄 **Multiple workflows**: Generate and share, generate only, or share existing PDFs
- 🧹 **Smart cleanup**: Automatic removal of old temporary files
- 🎯 **TypeScript support**: Full type definitions with IntelliSense

## Install

```bash
npm install capacitor-plugin-pdfshare
npx cap sync
```

## Quick Start

```typescript
import { PdfShare } from 'capacitor-plugin-pdfshare';

// Basic usage - generate and share PDF
await PdfShare.generateAndShare({
  filename: 'my-document',
  title: 'My Document'
});

// Generate PDF without sharing
const result = await PdfShare.generatePdfOnly({
  filename: 'report',
  format: 'letter',
  orientation: 'landscape'
});

if (result.success) {
  console.log('PDF saved to:', result.path);
}
```

## Configuration Examples

### Basic Configuration
```typescript
await PdfShare.generateAndShare({
  elementId: 'content',        // HTML element to convert
  filename: 'veterinary-report',
  title: 'Veterinary Report'
});
```

### Advanced Configuration
```typescript
await PdfShare.generateAndShare({
  elementId: 'printPage',
  filename: 'detailed-report',
  title: 'Detailed Veterinary Analysis',
  orientation: 'landscape',
  format: 'letter',
  margins: {
    top: 30,
    bottom: 30,
    left: 25,
    right: 25
  },
  quality: 0.95,
  includeBackground: true,
  scale: 2
});
```

### Share Existing PDF
```typescript
await PdfShare.shareExistingPdf({
  path: '/path/to/existing/file.pdf',
  title: 'Shared Document'
});
```

## Platform-Specific Notes

### iOS
- Uses native `UIPrintPageRenderer` for high-quality PDF generation
- Supports all page formats and orientations
- Integrates with native iOS share sheet

### Android
- Uses `PdfDocument` API for reliable PDF creation
- Automatic file provider configuration for secure sharing
- Supports all configuration options

### Web
- Fallback implementation using `html2pdf.js`
- Web Share API support where available
- Download link fallback for unsupported browsers

## API

<docgen-index>

* [`generateAndShare(...)`](#generateandshare)
* [`generatePdfOnly(...)`](#generatepdfonly)
* [`shareExistingPdf(...)`](#shareexistingpdf)
* [Interfaces](#interfaces)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### generateAndShare(...)

```typescript
generateAndShare(options?: PdfShareOptions | undefined) => Promise<PdfShareResult>
```

Generate PDF from HTML element and share it

| Param         | Type                                                        |
| ------------- | ----------------------------------------------------------- |
| **`options`** | <code><a href="#pdfshareoptions">PdfShareOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#pdfshareresult">PdfShareResult</a>&gt;</code>

--------------------


### generatePdfOnly(...)

```typescript
generatePdfOnly(options?: PdfShareOptions | undefined) => Promise<PdfShareResult>
```

Generate PDF without sharing (returns file path)

| Param         | Type                                                        |
| ------------- | ----------------------------------------------------------- |
| **`options`** | <code><a href="#pdfshareoptions">PdfShareOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#pdfshareresult">PdfShareResult</a>&gt;</code>

--------------------


### shareExistingPdf(...)

```typescript
shareExistingPdf(options: { path: string; title?: string; }) => Promise<PdfShareResult>
```

Share an existing PDF file

| Param         | Type                                           |
| ------------- | ---------------------------------------------- |
| **`options`** | <code>{ path: string; title?: string; }</code> |

**Returns:** <code>Promise&lt;<a href="#pdfshareresult">PdfShareResult</a>&gt;</code>

--------------------


### Interfaces


#### PdfShareResult

| Prop          | Type                 | Description                                      |
| ------------- | -------------------- | ------------------------------------------------ |
| **`success`** | <code>boolean</code> | Whether the operation was successful             |
| **`path`**    | <code>string</code>  | File path or URL (web only)                      |
| **`error`**   | <code>string</code>  | Error message if operation failed                |
| **`message`** | <code>string</code>  | Success message with details about the operation |


#### PdfShareOptions

| Prop                    | Type                                                                           | Description                                                          |
| ----------------------- | ------------------------------------------------------------------------------ | -------------------------------------------------------------------- |
| **`elementId`**         | <code>string</code>                                                            | HTML element ID to convert to PDF. Defaults to 'printPage'           |
| **`filename`**          | <code>string</code>                                                            | Output filename (without extension). Defaults to 'veterinary-dosage' |
| **`title`**             | <code>string</code>                                                            | Document title for sharing. Defaults to 'Veterinary Dosage'          |
| **`orientation`**       | <code>'portrait' \| 'landscape'</code>                                         | Page orientation. Defaults to 'portrait'                             |
| **`format`**            | <code>'a4' \| 'letter' \| 'legal'</code>                                       | Paper format. Defaults to 'a4'                                       |
| **`margins`**           | <code>{ top?: number; bottom?: number; left?: number; right?: number; }</code> | Page margins in mm. Defaults to 20mm on all sides                    |
| **`quality`**           | <code>number</code>                                                            | Image quality (0.1 to 1.0). Defaults to 0.98                         |
| **`includeBackground`** | <code>boolean</code>                                                           | Include background colors and images. Defaults to true               |
| **`scale`**             | <code>number</code>                                                            | Scale factor for rendering. Defaults to 2                            |

</docgen-api>
