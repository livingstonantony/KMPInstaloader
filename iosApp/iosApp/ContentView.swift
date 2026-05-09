
import SwiftUI
import Shared

struct ContentView: View {
    @State private var shortCode = ""
    
    // State to hold the fetched data
    @State private var post: InstaPost? = nil
    @State private var isLoading = false
    @State private var errorMessage: String? = nil
    
    // State for the pager
    @State private var currentPage = 0
    
    // Initialize the repository
    private let repository = InstaRepository()
    
    var body: some View {
        VStack {
            HStack {
                TextField("Enter insta url", text: $shortCode)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                
                Button(action: {
                    fetchInstagramPost()
                }) {
                    Image(systemName: "arrow.down.circle.fill")
                        .font(.system(size: 30))
                        .foregroundColor((isLoading || shortCode.isEmpty) ? .gray : .blue)
                }
                .disabled(isLoading || shortCode.isEmpty)
            }
            .padding()
            
            if isLoading {
                ProgressView("Fetching...")
                    .padding()
            }
            
            if let error = errorMessage {
                Text(error)
                    .foregroundColor(.red)
                    .font(.caption)
                    .padding()
            }
            
            // Display Results
            if let post = post {
                VStack(spacing: 12) {
                    
                    // MARK: - View Pager (TabView)
                    ZStack(alignment: .bottomLeading) {
                        TabView(selection: $currentPage) {
                            ForEach(0..<post.images.count, id: \.self) { index in
                                AsyncImage(url: URL(string: post.images[index])) { image in
                                    image.resizable()
                                        .scaledToFit()
                                        .padding(.horizontal, 12)
                                } placeholder: {
                                    ProgressView()
                                }
                                .tag(index) // Important for selection binding
                            }
                        }
                        .tabViewStyle(PageTabViewStyle(indexDisplayMode: .never)) // We build custom dots
                        .frame(height: 400)
                        .background(Color.black.opacity(0.05))
                        .cornerRadius(12)
                        
                        // Page Count Overlay (e.g., 1/5)
                        Text("\(currentPage + 1)/\(post.images.count)")
                            .font(.caption2)
                            .padding(8)
                            .background(Circle()
                                .fill(Color.black.opacity(0.6)))
                            .foregroundColor(.white)
                            .padding(12)
                    }
                    
                    // MARK: - Dot Indicators
                    HStack(spacing: 8) {
                        ForEach(0..<post.images.count, id: \.self) { index in
                            Circle()
                                .fill(currentPage == index ? Color.blue : Color.gray.opacity(0.5))
                                .frame(width: 8, height: 8)
                                .animation(.spring(), value: currentPage)
                        }
                    }
                }
            }
            
            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
    }
    
    // Function to call the KMP repository
    private func fetchInstagramPost() {
        isLoading = true
        errorMessage = nil
        currentPage = 0 // Reset pager on new fetch
        
        Task {
            do {
                let result = try await repository.getPost(url: shortCode)
                await MainActor.run {
                    self.post = result
                    self.isLoading = false
                }
            } catch {
                await MainActor.run {
                    self.errorMessage = "Error: \(error.localizedDescription)"
                    self.isLoading = false
                }
            }
        }
    }
}
