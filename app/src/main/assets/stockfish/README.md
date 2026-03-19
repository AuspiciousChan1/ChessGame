Place Stockfish Android binaries here before enabling engine analysis in production.

Expected asset layout:
- stockfish/arm64-v8a/stockfish
- stockfish/armeabi-v7a/stockfish

Requirements:
- The binary file name must be exactly `stockfish`
- The binary should be built for Android/Linux and marked executable after extraction
- `StockfishAnalyzerProvider` will extract the matching ABI binary into `Context.noBackupFilesDir/engines/<abi>/stockfish`

Notes:
- If no matching binary exists in assets, the app safely falls back to the built-in heuristic analyzer
- You can replace these binaries without changing Kotlin code as long as the asset paths stay the same

