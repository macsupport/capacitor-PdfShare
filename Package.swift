// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapacitorPluginPdfshare",
    platforms: [.iOS(.v15)],
    products: [
        .library(
            name: "CapacitorPluginPdfshare",
            targets: ["PdfSharePlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "8.0.0")
    ],
    targets: [
        .target(
            name: "PdfSharePlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/PdfSharePlugin"),
        .testTarget(
            name: "PdfSharePluginTests",
            dependencies: ["PdfSharePlugin"],
            path: "ios/Tests/PdfSharePluginTests")
    ]
)