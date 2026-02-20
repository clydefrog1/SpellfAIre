import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { Router } from '@angular/router';

import { AuthService } from '../services/auth.service';
import { ProfileService } from '../services/profile.service';

function passwordsMatchValidator(): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const newPw = group.get('newPassword')?.value;
    const confirm = group.get('confirmPassword')?.value;
    if (newPw && confirm && newPw !== confirm) {
      return { passwordsMismatch: true };
    }
    return null;
  };
}

@Component({
  selector: 'app-profile-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './profile-page.html',
  styleUrl: './profile-page.scss'
})
export class ProfilePage {
  private readonly auth = inject(AuthService);
  private readonly profileService = inject(ProfileService);
  private readonly router = inject(Router);

  readonly user = this.auth.user;

  readonly isSubmitting = signal(false);
  readonly successMessage = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly avatarPreview = signal<string | null>(null);

  private pendingAvatarBase64: string | null = null;

  readonly form = new FormGroup(
    {
      username: new FormControl('', {
        nonNullable: true,
        validators: [Validators.minLength(3), Validators.maxLength(30)]
      }),
      currentPassword: new FormControl('', { nonNullable: true }),
      newPassword: new FormControl('', {
        nonNullable: true,
        validators: [Validators.minLength(8), Validators.maxLength(72)]
      }),
      confirmPassword: new FormControl('', { nonNullable: true })
    },
    { validators: passwordsMatchValidator() }
  );

  ngOnInit(): void {
    const u = this.user();
    if (u) {
      this.form.controls.username.setValue(u.username);
      if (u.avatarBase64) {
        this.avatarPreview.set(u.avatarBase64);
      }
    }
  }

  onFileChange(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = () => {
      const dataUrl = reader.result as string;
      this.avatarPreview.set(dataUrl);
      this.pendingAvatarBase64 = dataUrl;
    };
    reader.readAsDataURL(file);
  }

  canSubmit(): boolean {
    return this.form.valid && !this.isSubmitting();
  }

  async submit(): Promise<void> {
    if (this.isSubmitting()) return;

    if (!this.form.valid) {
      this.form.markAllAsTouched();
      return;
    }

    const { username, currentPassword, newPassword } = this.form.getRawValue();

    const request: Record<string, string | null | undefined> = {};

    const currentUsername = this.user()?.username ?? '';
    if (username.trim() && username.trim() !== currentUsername) {
      request['username'] = username.trim();
    }
    if (newPassword) {
      request['currentPassword'] = currentPassword;
      request['newPassword'] = newPassword;
    }
    if (this.pendingAvatarBase64 !== null) {
      request['avatarBase64'] = this.pendingAvatarBase64;
    }

    if (Object.keys(request).length === 0) {
      this.successMessage.set('Nothing to update.');
      return;
    }

    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.isSubmitting.set(true);

    try {
      await this.profileService.updateProfile(request);
      this.successMessage.set('Profile updated successfully.');
      this.form.controls.currentPassword.reset('');
      this.form.controls.newPassword.reset('');
      this.form.controls.confirmPassword.reset('');
      this.pendingAvatarBase64 = null;
    } catch (err: unknown) {
      const status = (err as { status?: number })?.status;
      if (status === 400) {
        this.errorMessage.set('Current password is incorrect.');
      } else {
        this.errorMessage.set('Failed to update profile. Please try again.');
      }
    } finally {
      this.isSubmitting.set(false);
    }
  }

  goBack(): void {
    this.router.navigateByUrl('/');
  }
}
