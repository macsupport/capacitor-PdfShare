export interface PdfSharePlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
