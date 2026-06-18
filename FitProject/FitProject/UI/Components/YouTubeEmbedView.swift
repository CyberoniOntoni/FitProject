import SwiftUI
import WebKit

private let youtubeRefererHost = "com.fitproject.app"

enum YouTubeIds {
    private static let urlPattern = #/(?:youtube\.com/(?:watch\?v=|embed/|shorts/)|youtu\.be/|youtube-nocookie\.com/embed/)([A-Za-z0-9_-]{11})/#

    static func normalize(_ raw: String?) -> String? {
        guard let value = raw?.trimmingCharacters(in: .whitespacesAndNewlines), !value.isEmpty else {
            return nil
        }
        if let match = value.firstMatch(of: urlPattern) {
            return String(match.1)
        }
        return value
    }
}

enum YouTubeEmbedHTML {
    static func build(for youtubeId: String, autoplay: Bool = true) -> String {
        let autoplayParam = autoplay ? "1" : "0"
        let origin = "https://\(youtubeRefererHost)".addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
        return """
        <!DOCTYPE html>
        <html>
        <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <meta name="referrer" content="strict-origin-when-cross-origin">
        <style>
        html,body{margin:0;padding:0;width:100%;height:100%;background:#000;overflow:hidden}
        iframe{position:absolute;inset:0;width:100%;height:100%;border:0;background:#000}
        </style>
        </head>
        <body>
        <iframe
          src="https://www.youtube-nocookie.com/embed/\(youtubeId)?autoplay=\(autoplayParam)&rel=0&modestbranding=1&playsinline=1&enablejsapi=1&origin=\(origin)"
          title="Exercise video"
          allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
          referrerpolicy="strict-origin"
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
        webView.isOpaque = true
        webView.backgroundColor = .black
        webView.scrollView.backgroundColor = .black
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        let normalizedId = YouTubeIds.normalize(youtubeId) ?? youtubeId
        guard context.coordinator.loadedId != normalizedId else { return }
        context.coordinator.loadedId = normalizedId
        let html = YouTubeEmbedHTML.build(for: normalizedId)
        webView.loadHTMLString(html, baseURL: URL(string: "https://\(youtubeRefererHost)/"))
    }

    final class Coordinator {
        var loadedId: String?
    }
}

struct ExerciseVideoPreview: View {
    let exercise: FPWorkoutExercise
    @State private var isPlaying = false

    private var youtubeId: String? {
        YouTubeIds.normalize(exercise.youtubeId)
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