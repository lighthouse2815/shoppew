"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Button, Field } from "@shoppew/ui";
import { ShoppewApiError } from "@shoppew/api-client";
import { safeInternalPath } from "../lib/navigation";
import { useAuth } from "./providers";

const loginSchema = z.object({ email: z.email("Email chưa đúng định dạng."), password: z.string().min(1, "Nhập mật khẩu.") });
type LoginValues = z.infer<typeof loginSchema>;

export function LoginForm({ returnTo = "/account" }: { returnTo?: string }) {
  const router = useRouter();
  const { login } = useAuth();
  const { register, handleSubmit, formState: { errors, isSubmitting }, setError } = useForm<LoginValues>({ resolver: zodResolver(loginSchema) });
  const submit = handleSubmit(async (values) => {
    try {
      await login({ ...values, deviceName: "shoppew storefront" });
      router.replace(safeInternalPath(returnTo));
    } catch (error) {
      setError("root", { message: error instanceof ShoppewApiError ? error.message : "Không thể đăng nhập. Vui lòng thử lại." });
    }
  });
  return <form className="auth-form" method="post" onSubmit={submit} noValidate><Field label="Email" type="email" autoComplete="email" error={errors.email?.message} {...register("email")} /><Field label="Mật khẩu" type="password" autoComplete="current-password" error={errors.password?.message} {...register("password")} /><div className="auth-form__meta"><label><input type="checkbox" /> Ghi nhớ thiết bị</label><Link href="/forgot-password">Quên mật khẩu?</Link></div>{errors.root && <p className="notice notice--error" role="alert">{errors.root.message}</p>}<Button type="submit" disabled={isSubmitting}>{isSubmitting ? "Đang đăng nhập..." : "Đăng nhập"}</Button><p>Chưa có tài khoản? <Link href="/register">Đăng ký miễn phí</Link></p></form>;
}

const registerSchema = z.object({
  displayName: z.string().trim().min(1, "Nhập tên hiển thị.").max(120),
  email: z.email("Email chưa đúng định dạng."),
  phone: z.string().regex(/^$|^[0-9+() .-]{8,32}$/, "Số điện thoại chưa hợp lệ."),
  password: z.string().min(10, "Mật khẩu cần ít nhất 10 ký tự.").max(72),
  confirmation: z.string(),
}).refine((values) => values.password === values.confirmation, { path: ["confirmation"], message: "Mật khẩu xác nhận không khớp." });
type RegisterValues = z.infer<typeof registerSchema>;

export function RegisterForm() {
  const router = useRouter();
  const { register: createAccount } = useAuth();
  const { register, handleSubmit, formState: { errors, isSubmitting }, setError } = useForm<RegisterValues>({ resolver: zodResolver(registerSchema), defaultValues: { phone: "" } });
  const submit = handleSubmit(async (formValues) => {
    const { confirmation, ...values } = formValues;
    void confirmation;
    try {
      await createAccount({ ...values, phone: values.phone || undefined, deviceName: "shoppew storefront" });
      router.replace("/account");
    } catch (error) {
      setError("root", { message: error instanceof ShoppewApiError ? error.message : "Không thể tạo tài khoản. Vui lòng thử lại." });
    }
  });
  return <form className="auth-form" method="post" onSubmit={submit} noValidate><Field label="Tên hiển thị" autoComplete="name" error={errors.displayName?.message} {...register("displayName")} /><Field label="Email" type="email" autoComplete="email" error={errors.email?.message} {...register("email")} /><Field label="Số điện thoại (không bắt buộc)" type="tel" autoComplete="tel" error={errors.phone?.message} {...register("phone")} /><Field label="Mật khẩu" type="password" autoComplete="new-password" hint="Từ 10 đến 72 ký tự." error={errors.password?.message} {...register("password")} /><Field label="Nhập lại mật khẩu" type="password" autoComplete="new-password" error={errors.confirmation?.message} {...register("confirmation")} />{errors.root && <p className="notice notice--error" role="alert">{errors.root.message}</p>}<Button type="submit" disabled={isSubmitting}>{isSubmitting ? "Đang tạo tài khoản..." : "Tạo tài khoản"}</Button><p>Đã có tài khoản? <Link href="/login">Đăng nhập</Link></p></form>;
}
