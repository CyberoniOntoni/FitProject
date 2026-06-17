import SwiftUI
import WebKit

enum YouTubeEmbedHTML {
    static func build(for youtubeId: String, autoplay: Bool = true) -> String {
        let autoplayParam = autoplay ? "1" : "0"
        return """
        <!DOCTYPE html>
        <html>
        <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <meta name="referrer" content="strict-origin-when-cross-origin">
        <style>
        html,body{margin:0;padding:0;width:100%;height:100%;background:#000;overflow:hidden}
        iframe{position:absolute;inset:0;width:100%;height:100%;border:0}
        </style>
        </head>
        <body>
        <iframe
          src="https://www.youtube-nocookie.com/embed/\(youtubeId)?autoplay=\(autoplayParam)&rel=0&modestbranding=1&playsinline=1&enablejsapi=1"
          title="Exercise video"
          allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
          referrerpolicy="strict-origin-when-cross-origin"
          allowfullscreen>
        </iframe>
        </body>
        </html>
        """
    }
}

struct YouTubeEmbedView: UIViewRepresentable {
    let youtubeId: String

    func makeCoordinator() -> Coordinator { Coordinator() }

    func makeUIView(context: Context) -> WKWebView {
        let config = WKWebViewConfiguration()
        config.allowsInlineMediaPlayback = true
        config.mediaTypesRequiringUserActionForPlayback = []
        let webView = WKWebView(frame: .zero, configuration: config)
        webView.scrollView.isScrollEnabled = false
        webView.isOpaque = false
        webView.backgroundColor = .black
        webView.scrollView.backgroundColor = .black
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        guard context.coordinator.loadedId != youtubeId else { return }
        context.coordinator.loadedId = youtubeId
        let html = YouTubeEmbedHTML.build(for: youtubeId)
        webView.loadHTMLString(html, baseURL: URL(string: "https://fitproject.local"))
    }

    final class Coordinator {
        var loadedId: String?
    }
}

struct ExerciseVideoPreview: View {
    let exercise: FPWorkoutExercise
    @State private var isPlaying = false

    private var youtubeId: String? {
        guard let id = exercise.youtubeId, !id.isEmpty else { return nil }
        return id
    }

    var body: some View {
        Group {
            if youtubeId != nil {
                ZStack {
                    if isPlaying, let id = youtubeId {
                        YouTubeEmbedView(youtubeId: id)
                    } else if let url = exercise.videoThumbnailURL {
                        AsyncImage(url: url) { phase in
                            switch phase {
                            case .success(let image):
                                image.resizable().aspectRatio(16/9, contentMode: .fill)
                            default:
                                Rectangle().fill(BWSTheme.surfaceHighlight)
                            }
                        }

                        Color.black.opacity(0.35)

                        Image(systemName: "play.circle.fill")
                            .font(.system(size: 56))
                            .foregroundStyle(.white.opacity(0.9))
                    }
                }
                .frame(height: 200)
                .clipShape(RoundedRectangle(cornerRadius: BWSTheme.cardRadius))
                .contentShape(RoundedRectangle(cornerRadius: BWSTheme.cardRadius))
                .onTapGesture {
                    guard youtubeId != nil, !isPlaying else { return }
                    isPlaying = true
                }
            }
        }
        .onChange(of: exercise.id) { _, _ in
            isPlaying = false
        }
    }
}