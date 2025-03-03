#import <Foundation/Foundation.h>
#import <Capacitor/Capacitor.h>

CAP_PLUGIN(PdfSharePlugin, "PdfShare",
           CAP_PLUGIN_METHOD(generateAndShare, CAPPluginReturnPromise);
)
