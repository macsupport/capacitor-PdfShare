import { PdfShare } from 'capacitor-plugin-pdfshare';

window.testEcho = () => {
    const inputValue = document.getElementById("echoInput").value;
    PdfShare.echo({ value: inputValue })
}
