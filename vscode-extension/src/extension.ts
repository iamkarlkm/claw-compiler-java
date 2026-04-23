import * as vscode from 'vscode';
import * as path from 'path';
import { LanguageClient, LanguageClientOptions, ServerOptions, TransportKind } from 'vscode-languageserver/node';

// This method is called when your extension is activated
// Your extension is activated the very first time the command is executed
export function activate(context: vscode.ExtensionContext) {
    console.log('Claw Compiler Language Server is now active!');

    // Create the language server client
    const serverOptions: ServerOptions = {
        run: {
            command: 'java',
            args: [
                '-jar',
                path.join(context.extensionPath, 'server', 'claw-lsp-server.jar'),
                '--stdio'
            ]
        },
        debug: {
            command: 'java',
            args: [
                '-Xmx1G',
                '-agentlib:jdwp=transport=dt_socket,server=true,suspend=false,localport=5007',
                '-jar',
                path.join(context.extensionPath, 'server', 'claw-lsp-server.jar')
            ]
        }
    };

    const clientOptions: LanguageClientOptions = {
        // Identify the document types to support
        documentSelector: [{ scheme: 'file', language: 'claw' }],

        // Configure the client settings
        settings: {
            'claw': {
                serverPath: '',
                completionEnabled: true,
                diagnosticEnabled: true,
                definitionEnabled: true,
                referencesEnabled: true,
                hoverEnabled: true
            }
        },

        // Notify the client about the language server status
        synchronize: {
            // Configure the client to handle file changes
            fileEvents: vscode.workspace.createFileSystemWatcher('**/*.claw')
        }
    };

    // Create and start the language server
    const client = new LanguageClient(
        'clawCompiler',
        'Claw Compiler Language Server',
        serverOptions,
        clientOptions
    );

    // Register commands
    client.onReady().then(() => {
        // Restart server command
        const restartCommand = vscode.commands.registerCommand('claw.restartServer', () => {
            client.restart();
            vscode.window.showInformationMessage('Claw LSP server restarted');
        });

        // Show performance report command
        const performanceCommand = vscode.commands.registerCommand('claw.showPerformanceReport', async () => {
            try {
                const performanceReport = await client.sendRequest<string>('claw/showPerformanceReport');
                const doc = await vscode.window.showTextDocument(
                    await vscode.workspace.openTextDocument({
                        content: performanceReport,
                        language: 'plaintext'
                    }),
                    { preview: false, viewColumn: vscode.ViewColumn.Beside }
                );
                doc.edit(edit => {
                    edit.insert(new vscode.Position(0, 0), performanceReport);
                });
            } catch (error) {
                vscode.window.showErrorMessage(`Failed to show performance report: ${error}`);
            }
        });

        // Clear cache command
        const clearCacheCommand = vscode.commands.registerCommand('claw.clearCache', async () => {
            try {
                await client.sendRequest('claw/clearCache');
                vscode.window.showInformationMessage('Cache cleared successfully');
            } catch (error) {
                vscode.window.showErrorMessage(`Failed to clear cache: ${error}`);
            }
        });

        // Compile document command
        const compileCommand = vscode.commands.registerCommand('claw.compileDocument', async (uri: vscode.Uri) => {
            try {
                if (!uri) {
                    uri = vscode.window.activeTextEditor?.document.uri;
                }

                if (!uri) {
                    vscode.window.showErrorMessage('No file to compile');
                    return;
                }

                const result = await client.sendRequest<string>('claw/compileDocument', {
                    uri: uri.toString()
                });

                const doc = await vscode.window.showTextDocument(
                    await vscode.workspace.openTextDocument({
                        content: result,
                        language: 'plaintext'
                    }),
                    { preview: false, viewColumn: vscode.ViewColumn.Beside }
                );
                doc.edit(edit => {
                    edit.insert(new vscode.Position(0, 0), result);
                });

                vscode.window.showInformationMessage('Document compiled successfully');
            } catch (error) {
                vscode.window.showErrorMessage(`Failed to compile document: ${error}`);
            }
        });

        // Register all commands
        context.subscriptions.push(restartCommand);
        context.subscriptions.push(performanceCommand);
        context.subscriptions.push(clearCacheCommand);
        context.subscriptions.push(compileCommand);

        // Show welcome message
        vscode.window.showInformationMessage(
            'Claw Compiler Language Server is ready! Press F12 to jump to definition.',
            'Open Documentation'
        ).then(selection => {
            if (selection === 'Open Documentation') {
                vscode.commands.executeCommand('vscode.open', vscode.Uri.parse('https://github.com/claw-compiler/claw-compiler'));
            }
        });
    });

    // Start the client
    return client.start();
}

// This method is called when your extension is deactivated
export function deactivate(): Thenable<void> | void {
    return undefined;
}
