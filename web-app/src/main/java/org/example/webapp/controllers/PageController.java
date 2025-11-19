package org.example.webapp.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping
public class PageController {
    @GetMapping("/login")
    public String getLoginPage() {
        return "login";
    }

    @PreAuthorize("hasAuthority('client:view')")
    @GetMapping("/clients")
    public String getClientsPage() {
        return "clients";
    }

    @PreAuthorize("hasAuthority('client:view')")
    @GetMapping("/routes")
    public String getRoutesPage() {
        return "routes";
    }

    @PreAuthorize("hasAuthority('purchase:view')")
    @GetMapping("/purchase")
    public String getPurchasePage() {
        return "purchase";
    }

    @PreAuthorize("hasAuthority('sale:view')")
    @GetMapping("/sale")
    public String getSalePage() {
        return "sale";
    }

    @PreAuthorize("hasAuthority('container:view')")
    @GetMapping("/containers")
    public String getContainersPage() {
        return "containers";
    }

    @PreAuthorize("hasAuthority('inventory:view')")
    @GetMapping("/inventory")
    public String getInventoryPage() {
        return "inventory";
    }

    @PreAuthorize("hasAuthority('finance:view')")
    @GetMapping("/balance")
    public String getBalancePage() {
        return "balance";
    }

    @PreAuthorize("hasAuthority('finance:view')")
    @GetMapping("/finance")
    public String getFinancePage() {
        return "finance";
    }

    @PreAuthorize("hasAuthority('warehouse:view')")
    @GetMapping("/stock")
    public String getStockPage() {
        return "stock";
    }

    @PreAuthorize("hasAuthority('analytics:view')")
    @GetMapping("/analytics")
    public String getAnalyticsPage() {
        return "analytics";
    }

    @PreAuthorize("hasAuthority('settings:view')")
    @GetMapping("/settings")
    public String getSettingsPage() {
        return "settings";
    }

    @PreAuthorize("hasAuthority('administration:view')")
    @GetMapping("/administration")
    public String getAdministrationPage() {
        return "administration";
    }

    @GetMapping("/profile")
    public String getProfilePage() {
        return "profile";
    }
}
