import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, NavigationEnd } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { filter } from 'rxjs';

/**
 * Replaces the LMS-style top navbar with a fixed left sidebar — the
 * standard layout pattern for project/task management tools (Jira,
 * Asana, Linear, Notion) as opposed to LMS's course-platform top nav.
 * This is a structural difference, not just a color change.
 */
@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './sidebar.html'
})
export class Sidebar {
  hideSidebar = false;

  constructor(public auth: AuthService, private router: Router) {
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd)
    ).subscribe((event) => {
      this.hideSidebar = event.urlAfterRedirects.startsWith('/login')
                       || event.urlAfterRedirects.startsWith('/register');
    });
  }

  logout(): void {
    this.auth.logout();
  }
}