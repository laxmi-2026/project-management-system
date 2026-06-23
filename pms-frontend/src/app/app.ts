import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterOutlet, NavigationEnd } from '@angular/router';
import { Sidebar } from './components/sidebar/sidebar';
import { filter } from 'rxjs';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, Sidebar],
  templateUrl: './app.html'
})
export class App {
  /**
   * Tracks whether the current route is an auth page (login/register).
   * On those routes the sidebar is hidden AND the main content area
   * must lose its margin-left/sidebar-offset entirely, otherwise the
   * login card stays pushed to the right by the space the sidebar
   * would have occupied — even though the sidebar itself isn't
   * rendered. This was the root cause of the login page appearing
   * off-center instead of truly centered on screen.
   */
  isAuthPage = false;

  constructor(private router: Router) {
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd)
    ).subscribe((event) => {
      this.isAuthPage = event.urlAfterRedirects.startsWith('/login')
                      || event.urlAfterRedirects.startsWith('/register');
    });
  }
}