// Einzeldatei-Starter für Phishing for Compliments.
//
// Die fertige App (jpackage-Abbild inkl. Java-Laufzeit) steckt als "app.zip" in dieser .exe.
// Beim ersten Start wird sie nach %LOCALAPPDATA%\PhishingForCompliments\app-<build> entpackt,
// danach startet die .exe sofort. Neue Builds bekommen einen neuen Ordner, alte werden entfernt.
//
// Kompiliert mit dem in Windows enthaltenen C#-Compiler (.NET Framework 4.x), siehe build-exe.ps1.
// Bewusst nur C#-5-Syntax, damit das ohne zusätzliche SDKs funktioniert.

using System;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.IO.Compression;
using System.Reflection;
using System.Text;
using System.Threading;
using System.Windows.Forms;

[assembly: AssemblyTitle("Phishing for Compliments")]
[assembly: AssemblyProduct("Phishing for Compliments")]
[assembly: AssemblyDescription("Lernspiel zum Erkennen von Phishing-Mails")]
[assembly: AssemblyCompany("Uni-Projekt")]
[assembly: AssemblyVersion("1.0.0.0")]
[assembly: AssemblyFileVersion("1.0.0.0")]

static class PfcLauncher
{
    const string AppName = "PhishingForCompliments";
    const string ResourceName = "app.zip";

    [STAThread]
    static int Main(string[] args)
    {
        bool extractOnly = Array.IndexOf(args, "--extract-only") >= 0;
        bool selfTest = Array.IndexOf(args, "--selftest") >= 0;
        try
        {
            string baseDir = Environment.GetEnvironmentVariable("PFC_HOME");
            if (string.IsNullOrEmpty(baseDir))
            {
                baseDir = Path.Combine(
                    Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), AppName);
            }
            // Eindeutig pro Build: die Modul-ID ändert sich bei jeder Kompilierung.
            string build = typeof(PfcLauncher).Module.ModuleVersionId.ToString("N").Substring(0, 12);
            string appDir = Path.Combine(baseDir, "app-" + build);
            string exe = Path.Combine(appDir, AppName + ".exe");

            if (!File.Exists(exe))
            {
                if (extractOnly || selfTest)
                {
                    Extract(baseDir, appDir);
                }
                else
                {
                    ExtractWithSplash(baseDir, appDir);
                }
                RemoveOldBuilds(baseDir, appDir);
            }
            if (extractOnly)
            {
                return 0;
            }

            ProcessStartInfo psi = new ProcessStartInfo(exe);
            psi.UseShellExecute = false;
            psi.WorkingDirectory = appDir;
            psi.Arguments = JoinArguments(args);
            Process process = Process.Start(psi);
            if (selfTest)
            {
                process.WaitForExit();
                return process.ExitCode;
            }
            return 0;
        }
        catch (Exception ex)
        {
            if (!extractOnly && !selfTest)
            {
                MessageBox.Show("Phishing for Compliments konnte nicht gestartet werden:\n\n" + ex.Message,
                    "Phishing for Compliments", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
            Console.Error.WriteLine(ex);
            return 1;
        }
    }

    static void Extract(string baseDir, string appDir)
    {
        Directory.CreateDirectory(baseDir);
        string tmp = appDir + ".tmp-" + Guid.NewGuid().ToString("N").Substring(0, 8);
        string tmpRoot = Path.GetFullPath(tmp) + Path.DirectorySeparatorChar;
        Directory.CreateDirectory(tmp);
        try
        {
            using (Stream resource = Assembly.GetExecutingAssembly().GetManifestResourceStream(ResourceName))
            {
                if (resource == null)
                {
                    throw new InvalidOperationException("Die Spieldaten fehlen in der .exe.");
                }
                using (ZipArchive zip = new ZipArchive(resource, ZipArchiveMode.Read))
                {
                    foreach (ZipArchiveEntry entry in zip.Entries)
                    {
                        string target = Path.GetFullPath(Path.Combine(tmp, entry.FullName));
                        if (!target.StartsWith(tmpRoot, StringComparison.OrdinalIgnoreCase))
                        {
                            throw new InvalidOperationException("Ungültiger Pfad im Archiv: " + entry.FullName);
                        }
                        if (entry.FullName.EndsWith("/") || entry.FullName.EndsWith("\\"))
                        {
                            Directory.CreateDirectory(target);
                            continue;
                        }
                        Directory.CreateDirectory(Path.GetDirectoryName(target));
                        using (Stream input = entry.Open())
                        using (FileStream output = File.Create(target))
                        {
                            input.CopyTo(output);
                        }
                    }
                }
            }
            if (Directory.Exists(appDir))
            {
                // Ein zweiter, gleichzeitig gestarteter Starter war schneller.
                Directory.Delete(tmp, true);
            }
            else
            {
                Directory.Move(tmp, appDir);
            }
        }
        catch
        {
            TryDelete(tmp);
            throw;
        }
    }

    /** Erster Start: kleines Fenster anzeigen, während im Hintergrund entpackt wird. */
    static void ExtractWithSplash(string baseDir, string appDir)
    {
        Exception error = null;
        Form splash = new Form();
        splash.Text = "Phishing for Compliments";
        splash.FormBorderStyle = FormBorderStyle.FixedDialog;
        splash.StartPosition = FormStartPosition.CenterScreen;
        splash.ControlBox = false;
        splash.ClientSize = new Size(380, 96);
        splash.ShowInTaskbar = true;
        try
        {
            splash.Icon = Icon.ExtractAssociatedIcon(Assembly.GetExecutingAssembly().Location);
        }
        catch
        {
            // Icon ist nur Kosmetik.
        }

        Label label = new Label();
        label.Text = "Das Spiel wird beim ersten Start vorbereitet …";
        label.AutoSize = false;
        label.SetBounds(16, 14, 348, 24);
        ProgressBar bar = new ProgressBar();
        bar.Style = ProgressBarStyle.Marquee;
        bar.SetBounds(16, 48, 348, 20);
        splash.Controls.Add(label);
        splash.Controls.Add(bar);

        splash.Shown += delegate
        {
            Thread worker = new Thread(delegate ()
            {
                try
                {
                    Extract(baseDir, appDir);
                }
                catch (Exception ex)
                {
                    error = ex;
                }
                splash.BeginInvoke(new MethodInvoker(splash.Close));
            });
            worker.IsBackground = true;
            worker.Start();
        };
        Application.EnableVisualStyles();
        Application.Run(splash);
        if (error != null)
        {
            throw error;
        }
    }

    static void RemoveOldBuilds(string baseDir, string currentDir)
    {
        try
        {
            foreach (string dir in Directory.GetDirectories(baseDir, "app-*"))
            {
                if (!string.Equals(Path.GetFullPath(dir), Path.GetFullPath(currentDir), StringComparison.OrdinalIgnoreCase))
                {
                    TryDelete(dir);
                }
            }
        }
        catch
        {
            // Aufräumen ist optional (z.B. wenn eine alte Version noch läuft).
        }
    }

    static void TryDelete(string dir)
    {
        try
        {
            if (Directory.Exists(dir))
            {
                Directory.Delete(dir, true);
            }
        }
        catch
        {
            // ignorieren
        }
    }

    static string JoinArguments(string[] args)
    {
        StringBuilder sb = new StringBuilder();
        foreach (string arg in args)
        {
            if (arg == "--extract-only")
            {
                continue;
            }
            if (sb.Length > 0)
            {
                sb.Append(' ');
            }
            sb.Append('"').Append(arg.Replace("\"", "\\\"")).Append('"');
        }
        return sb.ToString();
    }
}
