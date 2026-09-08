"use client";

import { useCallback, useRef, useState } from "react";

type PreviewFile = {
  file: File;
  preview: string;
  id: string;
};

type UploadProgress = {
  id: string;
  name: string;
  progress: number;
  status: "pending" | "uploading" | "done" | "error";
  error?: string;
};

const ACCEPTED_TYPES = ["image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif", "image/svg+xml"];
const MAX_SIZE_MB = 10;
const MAX_SIZE_BYTES = MAX_SIZE_MB * 1024 * 1024;

interface ImageUploaderProps {
  productId?: string;
  onUpload: (file: File, opts?: { altText?: string; isPrimary?: boolean }) => Promise<void>;
  onUploadMultiple?: (files: File[]) => Promise<void>;
  multiple?: boolean;
  maxFiles?: number;
  disabled?: boolean;
  isPrimaryDefault?: boolean;
}

export default function ImageUploader({
  onUpload,
  onUploadMultiple,
  multiple = true,
  maxFiles = 10,
  disabled = false,
  isPrimaryDefault = false,
}: ImageUploaderProps) {
  const [previews, setPreviews] = useState<PreviewFile[]>([]);
  const [dragActive, setDragActive] = useState(false);
  const [progresses, setProgresses] = useState<UploadProgress[]>([]);
  const [globalError, setGlobalError] = useState("");
  const [altText, setAltText] = useState("");
  const [isPrimary, setIsPrimary] = useState(isPrimaryDefault);
  const [uploading, setUploading] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  const validateFile = (file: File): string | null => {
    if (!ACCEPTED_TYPES.includes(file.type.toLowerCase())) {
      return `Invalid type ${file.type}. Allowed: jpg, png, webp, gif, svg`;
    }
    if (file.size > MAX_SIZE_BYTES) {
      return `File too large (${(file.size / 1024 / 1024).toFixed(1)}MB). Max ${MAX_SIZE_MB}MB`;
    }
    if (file.size === 0) {
      return "File is empty";
    }
    return null;
  };

  const createPreviews = (files: File[]) => {
    const newPreviews: PreviewFile[] = files.map((file) => ({
      file,
      preview: URL.createObjectURL(file),
      id: `${file.name}-${file.size}-${Date.now()}-${Math.random()}`,
    }));
    setPreviews((prev) => {
      const combined = [...prev, ...newPreviews];
      if (combined.length > maxFiles) {
        setGlobalError(`Max ${maxFiles} files allowed. Extra files ignored.`);
        return combined.slice(0, maxFiles);
      }
      return combined;
    });
  };

  const handleFiles = useCallback(
    (files: FileList | File[]) => {
      setGlobalError("");
      const arr = Array.from(files);
      const valid: File[] = [];
      const errors: string[] = [];

      for (const file of arr) {
        const err = validateFile(file);
        if (err) {
          errors.push(`${file.name}: ${err}`);
        } else {
          valid.push(file);
        }
      }

      if (errors.length) {
        setGlobalError(errors.join(" • "));
      }

      if (valid.length === 0) return;

      if (!multiple && valid.length > 1) {
        setGlobalError("Multiple files not allowed — only first file will be used");
        createPreviews([valid[0]]);
      } else {
        createPreviews(valid);
      }
    },
    [multiple, maxFiles]
  );

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (!disabled) setDragActive(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    if (disabled) return;
    if (e.dataTransfer.files) {
      handleFiles(e.dataTransfer.files);
    }
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      handleFiles(e.target.files);
      e.target.value = "";
    }
  };

  const removePreview = (id: string) => {
    setPreviews((prev) => {
      const target = prev.find((p) => p.id === id);
      if (target) URL.revokeObjectURL(target.preview);
      return prev.filter((p) => p.id !== id);
    });
  };

  const clearAll = () => {
    previews.forEach((p) => URL.revokeObjectURL(p.preview));
    setPreviews([]);
    setGlobalError("");
    setProgresses([]);
  };

  const handleUpload = async () => {
    if (previews.length === 0) {
      setGlobalError("Select at least one image");
      return;
    }
    setUploading(true);
    setGlobalError("");

    const initialProgress: UploadProgress[] = previews.map((p) => ({
      id: p.id,
      name: p.file.name,
      progress: 0,
      status: "pending" as const,
    }));
    setProgresses(initialProgress);

    const toUpload = [...previews];

    try {
      if (onUploadMultiple && toUpload.length > 1) {
        setProgresses((prev) => prev.map((pr) => ({ ...pr, status: "uploading", progress: 30 })));
        await onUploadMultiple(toUpload.map((p) => p.file));
        setProgresses((prev) => prev.map((pr) => ({ ...pr, status: "done", progress: 100 })));
        toUpload.forEach((p) => URL.revokeObjectURL(p.preview));
        setPreviews([]);
        setProgresses([]);
        setAltText("");
      } else {
        const errorIds = new Set<string>();
        for (let i = 0; i < toUpload.length; i++) {
          const p = toUpload[i];
          setProgresses((prev) => prev.map((pr) => (pr.id === p.id ? { ...pr, status: "uploading", progress: 50 } : pr)));
          try {
            await onUpload(p.file, { altText: altText.trim() || undefined, isPrimary: isPrimary && i === 0 });
            setProgresses((prev) => prev.map((pr) => (pr.id === p.id ? { ...pr, status: "done", progress: 100 } : pr)));
          } catch (err) {
            errorIds.add(p.id);
            setProgresses((prev) =>
              prev.map((pr) => (pr.id === p.id ? { ...pr, status: "error", progress: 0, error: err instanceof Error ? err.message : "Upload failed" } : pr))
            );
          }
        }

        if (errorIds.size === 0) {
          toUpload.forEach((p) => URL.revokeObjectURL(p.preview));
          setPreviews([]);
          setProgresses([]);
          setAltText("");
        } else {
          setPreviews((prev) => {
            const kept = prev.filter((p) => errorIds.has(p.id));
            prev.filter((p) => !errorIds.has(p.id)).forEach((p) => URL.revokeObjectURL(p.preview));
            return kept;
          });
          setProgresses((prev) => prev.filter((pr) => errorIds.has(pr.id)));
          setGlobalError(`${errorIds.size} file(s) failed. Fix and retry.`);
        }
      }
    } catch (e) {
      setGlobalError(e instanceof Error ? e.message : "Upload failed");
    } finally {
      setUploading(false);
    }
  };

  const handleUploadClick = () => {
    if (previews.length === 0 && inputRef.current) {
      inputRef.current.click();
    } else {
      handleUpload();
    }
  };

  return (
    <div className="space-y-4">
      <div
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        onClick={() => !disabled && inputRef.current?.click()}
        className={`relative border-2 border-dashed rounded-xl p-6 text-center cursor-pointer transition-colors ${
          dragActive
            ? "border-[#b45309] bg-[#fef3c7]/20"
            : "border-[#d6d3d1] hover:border-[#b45309]/50 hover:bg-[#fafaf9] bg-[#fafaf9]/20"
        } ${disabled ? "opacity-50 cursor-not-allowed" : ""}`}
      >
        <input
          ref={inputRef}
          type="file"
          accept={ACCEPTED_TYPES.join(",")}
          multiple={multiple}
          onChange={handleInputChange}
          className="hidden"
          disabled={disabled}
        />

        <div className="flex flex-col items-center gap-2">
          <div className="w-12 h-12 rounded-xl bg-[#fef3c7] flex items-center justify-center">
            <span className="material-symbols-outlined text-[#b45309] text-[24px]">cloud_upload</span>
          </div>
          <div>
            <p className="font-semibold text-[13px] text-[#1c1917]">
              Drag & drop images here
            </p>
            <p className="text-[11px] text-[#57534e] mt-1">
              or <span className="text-[#b45309] font-semibold">click to browse</span> • {ACCEPTED_TYPES.map((t) => t.split("/")[1]).join(", ")} • Max {MAX_SIZE_MB}MB each • {multiple ? `Up to ${maxFiles} files` : "Single file"}
            </p>
          </div>
          <div className="flex items-center gap-2 mt-2 text-[11px] text-[#57534e]">
            <span className="inline-flex items-center gap-1 px-2 py-1 rounded-full bg-[#fafaf9] border border-[#d6d3d1]">
              <span className="material-symbols-outlined text-[14px]">verified</span> Auto-validated
            </span>
            <span className="inline-flex items-center gap-1 px-2 py-1 rounded-full bg-[#fafaf9] border border-[#d6d3d1]">
              <span className="material-symbols-outlined text-[14px]">image</span> Preview instantly
            </span>
          </div>
        </div>

        {dragActive && (
          <div className="absolute inset-0 bg-[#b45309]/10 rounded-xl flex items-center justify-center pointer-events-none">
            <p className="font-semibold text-[#b45309]">Drop to add images</p>
          </div>
        )}
      </div>

      {previews.length > 0 && (
        <div className="bg-[#fafaf9] rounded-xl p-3 border border-[#d6d3d1]/40 space-y-3">
          <div>
            <label className="block text-[11px] font-semibold text-[#1c1917] mb-1">Alt text (applied to first image if multiple, or all if bulk)</label>
            <input
              value={altText}
              onChange={(e) => setAltText(e.target.value)}
              placeholder="e.g. Front view, lifestyle shot"
              className="w-full bg-white border border-[#d6d3d1]/60 rounded-xl px-3 py-2 text-[12px] focus:border-[#b45309] outline-none"
              disabled={disabled || uploading}
            />
          </div>
          <label className="flex items-center gap-2 cursor-pointer">
            <input
              type="checkbox"
              checked={isPrimary}
              onChange={(e) => setIsPrimary(e.target.checked)}
              className="w-4 h-4 rounded text-[#b45309]"
              disabled={disabled || uploading}
            />
            <span className="text-[12px] text-[#1c1917]">Set first image as primary</span>
            <span className="ml-auto text-[11px] bg-[#fef3c7] text-[#b45309] px-1.5 py-0.5 rounded">Featured</span>
          </label>
        </div>
      )}

      {previews.length > 0 && (
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <h4 className="font-semibold text-[13px] text-[#1c1917] flex items-center gap-2">
              <span className="material-symbols-outlined text-[16px] text-[#b45309]">photo_library</span>
              Selected ({previews.length})
            </h4>
            <button
              onClick={clearAll}
              disabled={uploading}
              className="text-[11px] font-semibold text-[#b91c1c] hover:underline disabled:opacity-50"
            >
              Clear all
            </button>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 max-h-64 overflow-y-auto pr-1">
            {previews.map((p) => {
              const prog = progresses.find((pr) => pr.id === p.id);
              return (
                <div key={p.id} className="relative rounded-xl overflow-hidden border border-[#d6d3d1]/40 bg-[#fafaf9] group">
                  <div className="aspect-[4/3] relative bg-white">
                    {/* eslint-disable-next-line @next/next/no-img-element */}
                    <img src={p.preview} alt={p.file.name} className="w-full h-full object-cover" />
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        removePreview(p.id);
                      }}
                      disabled={uploading}
                      className="absolute top-1 right-1 w-6 h-6 rounded-full bg-black/60 text-white flex items-center justify-center hover:bg-error transition-colors disabled:opacity-50"
                      title="Remove"
                    >
                      <span className="material-symbols-outlined text-[14px]">close</span>
                    </button>
                    {prog && prog.status !== "pending" && (
                      <div className="absolute inset-x-0 bottom-0 h-1 bg-black/20">
                        <div
                          className={`h-full transition-all ${prog.status === "error" ? "bg-error" : prog.status === "done" ? "bg-[#15803d]" : "bg-[#b45309]"}`}
                          style={{ width: `${prog.progress}%` }}
                        />
                      </div>
                    )}
                    {prog?.status === "error" && (
                      <div className="absolute inset-0 bg-[#fee2e2]/80 flex items-center justify-center p-2">
                        <p className="text-[11px] text-[#b91c1c] font-semibold text-center">{prog.error}</p>
                      </div>
                    )}
                    {prog?.status === "done" && (
                      <div className="absolute top-1 left-1 w-6 h-6 rounded-full bg-[#15803d] text-white flex items-center justify-center">
                        <span className="material-symbols-outlined text-[14px]">check</span>
                      </div>
                    )}
                    {prog?.status === "uploading" && (
                      <div className="absolute inset-0 bg-black/30 flex items-center justify-center">
                        <span className="material-symbols-outlined animate-spin text-white">progress_activity</span>
                      </div>
                    )}
                  </div>
                  <div className="p-2">
                    <p className="text-[11px] font-medium text-[#1c1917] truncate" title={p.file.name}>
                      {p.file.name}
                    </p>
                    <p className="text-[10px] text-[#57534e]">
                      {(p.file.size / 1024).toFixed(0)}KB • {p.file.type.split("/")[1]}
                    </p>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {globalError && (
        <div className="bg-[#fef2f2] border border-[#fecaca] rounded-xl px-3 py-2 flex items-start gap-2 text-[#b91c1c] text-[12px]">
          <span className="material-symbols-outlined text-[16px] shrink-0 mt-0.5">error</span>
          <span>{globalError}</span>
        </div>
      )}

      {progresses.length > 0 && (
        <div className="space-y-1">
          {progresses.map((pr) => (
            <div key={pr.id} className="flex items-center gap-2 text-[11px]">
              <span className={`w-2 h-2 rounded-full shrink-0 ${pr.status === "done" ? "bg-[#15803d]" : pr.status === "error" ? "bg-error" : pr.status === "uploading" ? "bg-[#b45309] animate-pulse" : "bg-outline"}`} />
              <span className="flex-1 truncate text-[#57534e]">{pr.name}</span>
              <span className={`font-semibold ${pr.status === "error" ? "text-[#b91c1c]" : pr.status === "done" ? "text-[#15803d]" : "text-[#57534e]"}`}>
                {pr.status === "pending" ? "Queued" : pr.status === "uploading" ? `${pr.progress}%` : pr.status === "done" ? "Done" : "Failed"}
              </span>
            </div>
          ))}
        </div>
      )}

      <div className="flex gap-2">
        <button
          onClick={handleUploadClick}
          disabled={disabled || uploading || (previews.length === 0 && !inputRef.current)}
          className="flex-1 py-2.5 rounded-xl bg-[#b45309] text-white font-semibold text-[13px] hover:bg-[#92400e] disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-1.5"
        >
          {uploading ? (
            <>
              <span className="material-symbols-outlined animate-spin text-[16px]">progress_activity</span>
              Uploading...
            </>
          ) : previews.length > 0 ? (
            <>
              <span className="material-symbols-outlined text-[16px]">cloud_upload</span>
              Upload {previews.length} image{previews.length !== 1 ? "s" : ""}
            </>
          ) : (
            <>
              <span className="material-symbols-outlined text-[16px]">add_photo_alternate</span>
              Select images
            </>
          )}
        </button>
        {previews.length > 0 && (
          <button
            onClick={clearAll}
            disabled={uploading}
            className="px-4 py-2.5 rounded-xl border border-[#d6d3d1] hover:bg-[#fafaf9] text-[13px] font-semibold disabled:opacity-50"
          >
            Cancel
          </button>
        )}
      </div>

      <p className="text-[11px] text-[#57534e] text-center">
        Images are stored at <span className="font-mono text-[#b45309]">/uploads/products/{"{id}"}/</span> and served via <span className="font-mono">/uploads/**</span>
      </p>
    </div>
  );
}
