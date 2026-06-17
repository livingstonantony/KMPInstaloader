//
//  ContentView.swift
//  iosApp
//

import SwiftUI
import Shared

struct ContentView: View {

    @StateObject private var viewModel = InstaViewModel()

    @State private var shortCode = ""
    @State private var currentPage = 0

    var body: some View {

        NavigationStack {

            ScrollView {

                VStack(spacing: 20) {

                    // MARK: - Input Section
                    HStack(spacing: 12) {

                        TextField(
                            "Paste Instagram URL",
                            text: $shortCode
                        )
                        .textFieldStyle(.roundedBorder)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()

                        Button {

                            currentPage = 0

                            viewModel.fetchPost(
                                url: shortCode
                            )

                        } label: {

                            ZStack {

                                if viewModel.isLoading {

                                    ProgressView()
                                        .tint(.white)

                                } else {

                                    Image(
                                        systemName: "arrow.down.circle.fill"
                                    )
                                    .font(.system(size: 24))
                                }
                            }
                            .frame(width: 30, height: 30)
                        }
                        .buttonStyle(.borderedProminent)
                        .disabled(
                            viewModel.isLoading ||
                            shortCode.isEmpty
                        )
                    }
                    .padding(.horizontal)

                    // MARK: - Error
                    if let errorMessage = viewModel.errorMessage {

                        Label(
                            errorMessage,
                            systemImage: "exclamationmark.triangle.fill"
                        )
                        .font(.footnote)
                        .foregroundStyle(.red)
                        .frame(
                            maxWidth: .infinity,
                            alignment: .leading
                        )
                        .padding()
                        .background(.red.opacity(0.08))
                        .clipShape(
                            RoundedRectangle(
                                cornerRadius: 16,
                                style: .continuous
                            )
                        )
                        .padding(.horizontal)
                    }

                    // MARK: - Content
                    if let post = viewModel.post {

                        VStack(spacing: 16) {

                            // MARK: - Image Pager
                            ZStack(alignment: .bottomLeading) {

                                TabView(selection: $currentPage) {

                                    ForEach(
                                        0..<post.images.count,
                                        id: \.self
                                    ) { index in

                                        let imageUrl = post.images[index]

                                        ZStack(
                                            alignment: .bottomTrailing
                                        ) {

                                            AsyncImage(
                                                url: URL(
                                                    string: imageUrl
                                                )
                                            ) { phase in

                                                switch phase {

                                                case .success(let image):

                                                    image
                                                        .resizable()
                                                        .scaledToFit()

                                                case .failure(_):

                                                    ContentUnavailableView(
                                                        "Image unavailable",
                                                        systemImage: "photo"
                                                    )

                                                default:

                                                    ProgressView()
                                                }
                                            }
                                            .frame(
                                                maxWidth: .infinity
                                            )
                                            .frame(height: 420)
                                            .background(
                                                Color(
                                                    uiColor: .secondarySystemBackground
                                                )
                                            )

                                            // MARK: - Download Button
                                            Button {

                                                viewModel.downloadMedia(
                                                    url: imageUrl
                                                )

                                            } label: {

                                                Image(
                                                    systemName: "arrow.down"
                                                )
                                                .font(
                                                    .system(
                                                        size: 18,
                                                        weight: .semibold
                                                    )
                                                )
                                                .foregroundStyle(.white)
                                                .frame(
                                                    width: 44,
                                                    height: 44
                                                )
                                                .background(
                                                    .ultraThinMaterial
                                                )
                                                .background(
                                                    Color.black.opacity(0.25)
                                                )
                                                .clipShape(Circle())
                                            }
                                            .padding(16)
                                        }
                                        .clipShape(
                                            RoundedRectangle(
                                                cornerRadius: 24,
                                                style: .continuous
                                            )
                                        )
                                        .padding(.horizontal)
                                        .tag(index)
                                    }
                                }
                                .tabViewStyle(
                                    .page(
                                        indexDisplayMode: .never
                                    )
                                )
                                .frame(height: 420)

                                // MARK: - Page Count
                                Text(
                                    "\(currentPage + 1)/\(post.images.count)"
                                )
                                .font(
                                    .caption.weight(.semibold)
                                )
                                .foregroundStyle(.white)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 6)
                                .background(.ultraThinMaterial)
                                .background(
                                    Color.black.opacity(0.3)
                                )
                                .clipShape(Capsule())
                                .padding(28)
                            }

                            // MARK: - Indicators
                            HStack(spacing: 8) {

                                ForEach(
                                    0..<post.images.count,
                                    id: \.self
                                ) { index in

                                    Circle()
                                        .fill(
                                            currentPage == index
                                            ? Color.accentColor
                                            : Color.secondary.opacity(0.3)
                                        )
                                        .frame(
                                            width: currentPage == index
                                            ? 10
                                            : 8,
                                            height: currentPage == index
                                            ? 10
                                            : 8
                                        )
                                }
                            }
                            .animation(
                                .spring(
                                    response: 0.3,
                                    dampingFraction: 0.7
                                ),
                                value: currentPage
                            )
                        }
                    }
                }
                .padding(.vertical)
            }
            .navigationTitle("InstaLoader")
            .navigationBarTitleDisplayMode(.large)
            .background(
                Color(
                    uiColor: .systemGroupedBackground
                )
            )
        }
    }
}
