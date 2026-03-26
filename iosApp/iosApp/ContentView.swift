
import SwiftUI
import Shared

struct ContentView: View {
    @State private var shortCode = ""
    
    // State to hold the fetched data
    @State private var post: InstaPost? = nil
    @State private var isLoading = false
    @State private var errorMessage: String? = nil
    
    // Initialize the repository
    private let repository = InstaRepository()
    
    var body: some View {
        VStack {
            TextField("Enter insta url", text: $shortCode)
                .textFieldStyle(RoundedBorderTextFieldStyle())
                .padding()
            
            Button("Fetch Post") {
                fetchInstagramPost()
            }
            .buttonStyle(.borderedProminent)
            .disabled(isLoading || shortCode.isEmpty)
            
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
                ScrollView {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("Caption:").font(.headline)
                        Text(post.caption)
                        
                        Text("Images:").font(.headline)
                        ForEach(post.images, id: \.self) { imageUrl in
                            AsyncImage(url: URL(string: imageUrl)) { image in
                                image.resizable()
                                    .scaledToFit()
                                    .cornerRadius(10)
                            } placeholder: {
                                ProgressView()
                            }
                            .frame(maxHeight: 300)
                        }
                    }
                    .padding()
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .padding()
    }
    
    // Function to call the KMP repository
    private func fetchInstagramPost() {
        isLoading = true
        errorMessage = nil
        
        Task {
            do {
                // repository.getPost is a suspend function, so we use 'try await'
                let result = try await repository.getPost(url: shortCode)
                
                // Update UI on the main thread
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

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
