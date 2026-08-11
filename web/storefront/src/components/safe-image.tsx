"use client";

import Image, { type ImageProps } from "next/image";
import { ImageIcon } from "lucide-react";
import { useState } from "react";

export function SafeImage({ fallback = "Ảnh đang được cập nhật", alt, ...props }: ImageProps & { fallback?: string }) {
  const [failed, setFailed] = useState(false);
  if (failed || !props.src) return <span className="image-fallback" role="img" aria-label={fallback}><ImageIcon aria-hidden="true" /><small>{fallback}</small></span>;
  return <Image alt={alt} {...props} onError={() => setFailed(true)} />;
}
