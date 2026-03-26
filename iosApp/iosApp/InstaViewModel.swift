//
//  InstaViewModel.swift
//  iosApp
//
//  Created by Philosopher on 24/03/26.
//

import SwiftUI
import Shared

@MainActor
class InstaViewModel: ObservableObject {
    @Published var post: InstaPost? = nil
    @Published var isLoading = false
    @Published var errorMessage: String? = nil
    @Published var downloadedData: Data? = nil

    private let repository = InstaRepository()

    func fetchPost(url: String) {
        isLoading = true
        errorMessage = nil
        
        Task {
            do {
                // Calling the suspend function getPost
                let result = try await repository.getPost(url: url)
                self.post = result
                self.isLoading = false
            } catch {
                self.errorMessage = error.localizedDescription
                self.isLoading = false
                print("Error: \(error)")
            }
        }
    }

    func downloadMedia(url: String) {
        Task {
            do {
                // Calling the suspend function downloadFile
                // Kotlin's ByteArray is mapped to Swift's Data (or KotlinByteArray)
                let nsData = try await repository.downloadFile(url: url)
                // Convert Kotlin ByteArray to Swift Data
//                self.downloadedData = Data(nsData)
            } catch {
                print("Download failed: \(error)")
            }
        }
    }
}
