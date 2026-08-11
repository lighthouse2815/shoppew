"use client";

import { useQuery } from "@tanstack/react-query";
import { Search } from "lucide-react";
import { useRouter } from "next/navigation";
import { FormEvent, useEffect, useId, useState } from "react";
import { publicApi, queryString } from "@/lib/api";

export function SearchBox() {
  const router = useRouter();
  const listId = useId();
  const [query, setQuery] = useState("");
  const [debounced, setDebounced] = useState("");
  const [focused, setFocused] = useState(false);

  useEffect(() => {
    const timer = window.setTimeout(() => setDebounced(query.trim()), 180);
    return () => window.clearTimeout(timer);
  }, [query]);

  const suggestions = useQuery({
    queryKey: ["search-suggestions", debounced],
    queryFn: () => publicApi.request<string[]>(`/api/v1/public/search/suggestions${queryString({ q: debounced, size: 8 })}`),
    enabled: debounced.length >= 2,
    staleTime: 60_000,
  });

  const openResult = (value: string) => {
    setQuery(value);
    setFocused(false);
    router.push(`/search${queryString({ q: value })}`);
  };
  const submit = (event: FormEvent) => {
    event.preventDefault();
    if (query.trim()) openResult(query.trim());
  };

  const showSuggestions = focused && debounced.length >= 2;
  return (
    <form
      className="search-box"
      role="search"
      action="/search"
      method="get"
      onSubmit={submit}
      onFocus={() => setFocused(true)}
      onBlur={(event) => {
        if (!event.currentTarget.contains(event.relatedTarget as Node | null)) setFocused(false);
      }}
    >
      <Search aria-hidden="true" />
      <input
        role="combobox"
        name="q"
        value={query}
        onChange={(event) => setQuery(event.target.value)}
        placeholder="Tìm sản phẩm, thương hiệu..."
        aria-label="Từ khóa tìm kiếm"
        aria-autocomplete="list"
        aria-haspopup="listbox"
        aria-controls={listId}
        aria-expanded={showSuggestions}
      />
      <button type="submit">Tìm kiếm</button>
      {showSuggestions && (
        <div className="search-suggestions" id={listId} role="listbox" aria-label="Gợi ý tìm kiếm">
          {suggestions.isPending ? (
            <span>Đang tìm gợi ý...</span>
          ) : suggestions.error ? (
            <span>Không tải được gợi ý. Bạn vẫn có thể nhấn Tìm kiếm.</span>
          ) : suggestions.data?.length ? (
            suggestions.data.map((suggestion) => (
              <button key={suggestion} type="button" role="option" aria-selected="false" onClick={() => openResult(suggestion)}>
                <Search aria-hidden="true" /> {suggestion}
              </button>
            ))
          ) : (
            <span>Không có gợi ý phù hợp.</span>
          )}
        </div>
      )}
    </form>
  );
}
